import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReservaService {
  // Esta URL apunta al backend que está corriendo en el puerto 8080
  private apiUrl = 'http://localhost:8080/api/reservas';

  constructor(private http: HttpClient) { }

  crearReserva(reserva: any): Observable<any> {
    return this.http.post(this.apiUrl, reserva);
  }
}