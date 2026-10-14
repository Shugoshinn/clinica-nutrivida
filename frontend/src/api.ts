import { msalInstance } from './auth';

const apiUrl = 'https://zphgtogiuc.execute-api.us-east-1.amazonaws.com/prod';

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
}

export interface Nutricionista {
  id?: number;
  nombre: string;
  especialidad: string;
  email: string;
}

async function request<T>(
  path: string,
  init?: RequestInit
): Promise<T> {
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');

  // 1. Obtener la sesión actual del usuario
  const accounts = msalInstance.getAllAccounts();
  
  if (accounts.length > 0) {
    try {
      // 2. Solicitar el token de manera silenciosa
      const tokenResponse = await msalInstance.acquireTokenSilent({
        scopes: ["api://db5f13af-8b1a-4d80-9944-17ea6ff024cf/access_as_user"],
        account: accounts[0]
      });
      
      // 3. Adjuntar el JWT en la cabecera Authorization
      headers.set('Authorization', `Bearer ${tokenResponse.accessToken}`);
    } catch (error) {
      console.warn("Fallo el token silencioso, abriendo ventana para consentimiento...", error);
      try {
        // 4. RESPALDO INTERACTIVO: Si falta consentimiento, abre el popup de Azure
        const tokenResponse = await msalInstance.acquireTokenPopup({
          scopes: ["api://db5f13af-8b1a-4d80-9944-17ea6ff024cf/access_as_user"]
        });
        headers.set('Authorization', `Bearer ${tokenResponse.accessToken}`);
      } catch (popupError) {
        console.error("Error crítico obteniendo el token JWT por popup:", popupError);
      }
    }
  }

  const response = await fetch(`${apiUrl}/api${path}`, { ...init, headers });
  
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