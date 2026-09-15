import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ClinicaService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Obtener todas las fichas nutricionales
  obtenerFichas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/fichas`);
  }

  // Crear una nueva ficha nutricional
  crearFicha(ficha: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/fichas`, ficha);
  }
}