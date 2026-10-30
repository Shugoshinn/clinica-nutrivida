import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { msalInstance } from './auth';
import { environment } from './environments/environment';

export const apiUrl = environment.apiUrl;

export interface Reserva {
  id?: number;
  tipoAtencion: string;
  modalidad: string;
  fecha: string;
  hora: string;
  pacienteNombre: string;
  nutricionista?: string;
  nutricionistaTemp?: string;
  estado?: string;
}

export interface Ficha {
  id?: number;
  pacienteRut: string;
  motivoConsulta: string;
  antecedentes: string;
  pesoActual: number | null;
  estatura: number | null;
  // Calculados de forma asíncrona por el consumidor Kafka de fichas
  imc?: number | null;
  clasificacionImc?: string | null;
}

export interface Nutricionista {
  id?: number;
  nombre: string;
  especialidad: string;
  email: string;
}

// Obtiene el JWT de Azure AD para la API: primero en silencio y, si requiere interacción, por popup
export async function getAccessToken(): Promise<string | null> {
  const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!account) return null;

  try {
    const tokenResponse = await msalInstance.acquireTokenSilent({ scopes: environment.apiScopes, account });
    return tokenResponse.accessToken;
  } catch (error) {
    if (!(error instanceof InteractionRequiredAuthError)) throw error;
    console.warn('Se requiere consentimiento, abriendo ventana de Azure...', error);
    const tokenResponse = await msalInstance.acquireTokenPopup({ scopes: environment.apiScopes, account });
    return tokenResponse.accessToken;
  }
}

// Cliente HTTP: adjunta el JWT en el header Authorization de cada llamada al backend
async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');

  const accessToken = await getAccessToken();
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);

  const response = await fetch(`${apiUrl}/api${path}`, { ...init, headers });
  
  if (response.status === 401) throw new Error('Error 401: sesión expirada o token inválido');
  if (response.status === 403) throw new Error('Error 403: no tienes permisos para esta acción');
  if (!response.ok) throw new Error(`Error ${response.status}: ${response.statusText}`);
  if (response.status === 204) return undefined as T;

  // Tolerante a respuestas en texto plano (RabbitMQ) y JSON
  const text = await response.text();
  try {
    return text ? (JSON.parse(text) as T) : (undefined as T);
  } catch (error) {
    return text as unknown as T;
  }
}

export const clinicApi = {
  fichas: () => request<Ficha[]>('/fichas'),
  crearFicha: (ficha: Ficha) =>
    request<Ficha>('/fichas', { method: 'POST', body: JSON.stringify(ficha) }),
  reservas: async () => {
    const reservas = await request<Array<Reserva & { nutricionistaNombre?: string }>>('/reservas');
    return reservas.map(({ nutricionistaNombre, ...reserva }) => ({
      ...reserva,
      nutricionista: nutricionistaNombre
    }));
  },
  crearReserva: (reserva: Reserva) =>
    request<Reserva>('/reservas', { method: 'POST', body: JSON.stringify(reserva) }),
  asignarNutricionista: (reserva: Reserva) =>
    request<Reserva>(`/reservas/${reserva.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...reserva,
        nutricionistaNombre: reserva.nutricionistaTemp
      })
    }),
  nutricionistas: () => request<Nutricionista[]>('/nutricionistas'),
  crearNutricionista: (nutricionista: Nutricionista) =>
    request<Nutricionista>('/nutricionistas', {
      method: 'POST',
      body: JSON.stringify(nutricionista)
    }),
  eliminarNutricionista: (id: number) =>
    request<void>(`/nutricionistas/${id}`, { method: 'DELETE' })
};