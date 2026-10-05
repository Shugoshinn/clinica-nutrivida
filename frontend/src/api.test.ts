import { afterEach, describe, expect, it, vi } from 'vitest';
import { clinicApi, type Reserva } from './api';
import { roleFor } from './App';

describe('clinicApi', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('creates a reservation with the expected endpoint and payload', async () => {
    const reserva: Reserva = {
      tipoAtencion: 'Primera vez',
      modalidad: 'Presencial',
      fecha: '2026-10-15',
      hora: '10:30',
      pacienteNombre: 'Paciente de prueba'
    };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(reserva), {
      status: 201,
      headers: { 'Content-Type': 'application/json' }
    }));
    vi.stubGlobal('fetch', fetchMock);

    await expect(clinicApi.crearReserva(reserva)).resolves.toEqual(reserva);

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toBe('/api/reservas');
    expect(options.method).toBe('POST');
    expect(options.body).toBe(JSON.stringify(reserva));
    expect(new Headers(options.headers).has('Authorization')).toBe(false);
  });

  it('reports unsuccessful API responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 503, statusText: 'Unavailable' })));

    await expect(clinicApi.reservas()).rejects.toThrow('Error 503: Unavailable');
  });

  it('maps the backend nutritionist field into the reservation model', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify([{
      id: 7,
      pacienteNombre: 'Paciente de prueba',
      tipoAtencion: 'Control',
      modalidad: 'Online',
      fecha: '2026-10-15',
      hora: '10:30',
      nutricionistaNombre: 'Dra. Gómez',
      estado: 'Pendiente'
    }]), { status: 200, headers: { 'Content-Type': 'application/json' } })));

    await expect(clinicApi.reservas()).resolves.toMatchObject([
      { id: 7, nutricionista: 'Dra. Gómez', estado: 'Pendiente' }
    ]);
  });

  it('creates and deletes nutritionists through the API', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 9, nombre: 'Dra. Navarro', especialidad: 'Clínica', email: 'navarro@nutrivida.cl' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    const nutritionist = { nombre: 'Dra. Navarro', especialidad: 'Clínica', email: 'navarro@nutrivida.cl' };
    await expect(clinicApi.crearNutricionista(nutritionist)).resolves.toMatchObject({ id: 9 });
    await expect(clinicApi.eliminarNutricionista(9)).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/nutricionistas');
    expect(fetchMock.mock.calls[1]?.[0]).toBe('/api/nutricionistas/9');
  });

  it('recognizes the admin role for the control panel', () => {
    expect(roleFor('admin@nutrivida.cl')).toBe('Administrador');
    expect(roleFor('nicolas28rubilar02@gmail.com')).toBe('Administrador');
  });
});