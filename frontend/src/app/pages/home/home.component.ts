import { Component, OnInit, OnDestroy } from '@angular/core';
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

  private readonly _destroy$ = new Subject<void>();

  constructor(
    private reservaService: ReservaService,
    private clinicaService: ClinicaService,
    private authService: MsalService,
    private msalBroadcastService: MsalBroadcastService
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

      if (cuentaActiva.idTokenClaims && cuentaActiva.idTokenClaims['roles']) {
        const roles = cuentaActiva.idTokenClaims['roles'] as string[];
        this.userRole = roles[0];
      }
    }
  }

  iniciarSesion() {
    this.authService.loginRedirect();
  }

  cerrarSesion() {
    this.authService.logoutPopup().subscribe(() => {
      this.isLoggedIn = false;
      this.userName = '';
      this.userRole = '';
      this.fichas = [];
    });
  }

  cargarFichasSiEsNutricionista() {
    this.clinicaService.obtenerFichas().subscribe({
      next: (data) => this.fichas = data,
      error: (err) => console.error('Error al cargar fichas:', err)
    });
  }

  onSubmit() {
    this.reservaService.crearReserva(this.reserva).subscribe({
      next: (respuesta) => {
        alert('¡Reserva creada con éxito!');
        console.log(respuesta);
      },
      error: (err) => {
        alert('Error al crear la reserva: ' + (err.error?.message || err.message));
        console.error(err);
      }
    });
  }
}