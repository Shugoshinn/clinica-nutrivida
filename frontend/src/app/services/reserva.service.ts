import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ReservaService {
  // Esta URL ahora apunta a tu backend alojado en AWS
  private apiUrl = 'http://api-nutrivida-v2.us-east-1.elasticbeanstalk.com/api/reservas';

  constructor(private http: HttpClient) { }

  crearReserva(reserva: any): Observable<any> {
    return this.http.post(this.apiUrl, reserva);
  }

  obtenerReservas(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  asignarNutricionista(reserva: any): Observable<any> {
    // Le asignamos el valor temporal al campo nutricionista del objeto
    reserva.nutricionista = reserva.nutricionistaTemp;
    
    // Hace un PUT a http://.../api/reservas/{id}
    return this.http.put(`${this.apiUrl}/${reserva.id}`, reserva);
  }
}