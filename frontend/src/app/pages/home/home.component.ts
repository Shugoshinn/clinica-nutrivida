import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms'; 
import { ReservaService } from '../../services/reserva.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [FormsModule], 
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent {
  
  // Objeto que guardará los datos del formulario
  reserva = {
    tipoAtencion: '',
    modalidad: '',
    fecha: '',
    hora: '',
    pacienteNombre: 'Paciente Prueba' 
  };

  constructor(private reservaService: ReservaService) {}

  onSubmit() {
    this.reservaService.crearReserva(this.reserva).subscribe({
      next: (respuesta) => {
        alert('¡Reserva creada con éxito!');
        console.log(respuesta);
      },
      error: (err) => {
        alert('Error al crear la reserva: ' + err.error);
        console.error(err);
      }
    });
  }
}