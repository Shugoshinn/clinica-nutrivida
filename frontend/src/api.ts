const apiUrl = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

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

  const response = await fetch(`${apiUrl}/api${path}`, { ...init, headers });
  if (!response.ok) throw new Error(`Error ${response.status}: ${response.statusText}`);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
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
