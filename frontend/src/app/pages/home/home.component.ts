import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { ReservaService } from '../../services/reserva.service';
import { ClinicaService } from '../../services/clinica.service';
import { MsalService, MsalBroadcastService } from '@azure/msal-angular';
import { EventMessage, EventType, AuthenticationResult } from '@azure/msal-browser';
import { Subject } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule], 
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit, OnDestroy {
  
  isLoggedIn = false;
  userName = '';
  userRole = '';
  fichas: any[] = [];
  reserva = {
    tipoAtencion: '',
    modalidad: '',
    fecha: '',
    hora: '',
    pacienteNombre: '' 
  };
  nuevaFicha = {
    pacienteRut: '',
    motivoConsulta: '',
    antecedentes: '',
    pesoActual: null,
    // Puedes agregar los demás campos aquí (estatura, cintura, etc.) si quieres ampliarlos después
  };
  listaReservas: any[] = []; // <--- Para guardar la lista que viene de AWS

  private readonly _destroy$ = new Subject<void>();

  constructor(
    private reservaService: ReservaService,
    private clinicaService: ClinicaService,
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.verificarCuenta();
    this.cargarFichasSiEsNutricionista();

    // Escuchar cuando el login se complete para actualizar la vista al instante
    this.msalBroadcastService.msalSubject$
      .pipe(
        filter((msg: EventMessage) => msg.eventType === EventType.LOGIN_SUCCESS),
        takeUntil(this._destroy$)
      )
      .subscribe((result: EventMessage) => {
        const payload = result.payload as AuthenticationResult;
        this.authService.instance.setActiveAccount(payload.account);
        this.verificarCuenta();
        this.cargarFichasSiEsNutricionista();

        // NUEVO: Auto-scroll a la sección de reservas si NO es Nutricionista
        if (this.userRole !== 'Nutricionista') {
          setTimeout(() => {
            const reservaSection = document.getElementById('reservas');
            if (reservaSection) {
              // Esto hace que la página se deslice suavemente hasta el formulario
              reservaSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
          }, 500); // Esperamos medio segundo para que Angular alcance a "dibujar" el formulario oculto
        }
      });
  }

  ngOnDestroy(): void {
    this._destroy$.next();
    this._destroy$.complete();
  }

  verificarCuenta() {
    const cuentas = this.authService.instance.getAllAccounts();
    if (cuentas.length > 0) {
      this.isLoggedIn = true;
      const cuentaActiva = cuentas[0];
      this.userName = cuentaActiva.name || 'Usuario';
      this.reserva.pacienteNombre = this.userName;

      // 1. Capturamos el correo de la persona que inició sesión en Azure
      const correoUsuario = cuentaActiva.username ? cuentaActiva.username.toLowerCase() : '';
      console.log("Usuario logueado con el correo:", correoUsuario);

      // 2. Asignamos el rol según el correo exacto con el que entres por Azure
      if (correoUsuario === 'becerrajohan34@gmail.com') {
        // Si inicias sesión con este correo, tendrás vista de Nutricionista
        this.userRole = 'Nutricionista';
      } 
      else if (correoUsuario === 'luis.concha.d5@gmail.com') { 
        // Si inicias sesión con este correo de Azure, tendrás vista de Secretaria
        this.userRole = 'Secretaria';
        this.cargarTodasLasReservas();
      } 
      else {
        // Cualquier otro correo (Gmail, Hotmail u otro externo) será Paciente automáticamente
        this.userRole = 'Paciente';
      }
    }
  }

  iniciarSesion() {
    this.authService.loginRedirect();
  }

  // Método para cerrar la sesión de Microsoft y limpiar la interfaz
  cerrarSesion() {
    // Llamamos a MSAL para cerrar la ventana de sesión de Microsoft
    this.authService.logoutPopup({
      postLogoutRedirectUri: 'http://localhost:4200' // A dónde vuelve tras salir
    });

    // Limpiamos las variables locales de la aplicación
    this.isLoggedIn = false;
    this.userRole = '';
    this.userName = '';
    this.listaReservas = [];
    
    console.log('Sesión cerrada con éxito');
  }

  cargarFichasSiEsNutricionista() {
    this.clinicaService.obtenerFichas().subscribe({
      next: (data) => {
        this.fichas = data;
        console.log('¡Datos reales recibidos de AWS:', this.fichas); // <--- Aquí ya tendrá la información cuando llegue
      
        this.cdr.detectChanges(); // Forzar la detección de cambios para actualizar la vista
      },
      error: (err) => console.error('Error al cargar fichas:', err)
    });
  }

  onSubmit() {
    // Verificación básica para que no manden el formulario vacío
    if (!this.reserva.tipoAtencion || !this.reserva.fecha || !this.reserva.hora) {
      alert('Por favor, completa todos los campos obligatorios.');
      return;
    }
    // 1. VALIDACIÓN DE FECHA (No permitir fechas en el pasado)
    // Obtenemos la fecha de hoy en formato YYYY-MM-DD
    const fechaHoy = new Date().toISOString().split('T')[0]; 
    
    if (this.reserva.fecha < fechaHoy) {
      alert('No puedes agendar una hora en el pasado. Elige una fecha a partir de hoy.');
      return;
    }

    // 2. VALIDACIÓN DE HORA (Entre 09:00 y 20:00)
    if (this.reserva.hora < '09:00' || this.reserva.hora > '20:00') {
      alert('El horario de atención de la clínica es entre las 09:00 y las 20:00 hrs.');
      return;
    }

    this.reservaService.crearReserva(this.reserva).subscribe({
      next: (respuesta) => {
        alert('¡Tu hora ha sido agendada con éxito!');
        
        // Limpiamos el formulario para que quede en blanco otra vez
        this.reserva = {
          tipoAtencion: '',
          modalidad: '',
          fecha: '',
          hora: '',
          pacienteNombre: this.userName // Mantenemos el nombre del paciente logueado
        };
      },
      error: (err) => {
        console.error('Error al guardar la reserva:', err);
        alert('Hubo un problema al agendar tu hora. Intenta nuevamente.');
      }
    });
  }

  guardarFicha() {
    // Verificación básica
    if(!this.nuevaFicha.pacienteRut || !this.nuevaFicha.pesoActual) {
      alert("El RUT y el peso son obligatorios");
      return;
    }

    this.clinicaService.crearFicha(this.nuevaFicha).subscribe({
      next: (respuesta) => {
        alert('¡Ficha creada exitosamente en AWS!');
        // Volvemos a cargar las fichas para que la tabla se actualice
        this.cargarFichasSiEsNutricionista(); 
        
        // Limpiamos el formulario
        this.nuevaFicha = { pacienteRut: '', motivoConsulta: '', antecedentes: '', pesoActual: null };
      },
      error: (err) => {
        console.error('Error al guardar la ficha:', err);
        alert('Hubo un error al guardar la ficha.');
      }
    });
  }
  // Cargar todas las reservas para la vista de la secretaria
  cargarTodasLasReservas() {
    this.reservaService.obtenerReservas().subscribe({
      next: (data) => {
        this.listaReservas = data;
        console.log('Reservas cargadas para la secretaria:', this.listaReservas);
        this.cdr.detectChanges(); // Forzar actualización visual
      },
      error: (err) => {
        console.error('Error al cargar las reservas:', err);
      }
    });
  }

  // Asignar el nutricionista a una reserva específica
  asignar(res: any) {
    if (!res.nutricionistaTemp) {
      alert('Por favor, selecciona un nutricionista de la lista.');
      return;
    }

    // Le pasamos el objeto 'res' completo al servicio
    this.reservaService.asignarNutricionista(res).subscribe({
      next: (respuesta) => {
        alert('¡Nutricionista asignado con éxito!');
        res.nutricionista = res.nutricionistaTemp; // Actualizamos localmente en la tabla
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al asignar nutricionista:', err);
        alert('Hubo un error al guardar la asignación.');
      }
    });
  }

}