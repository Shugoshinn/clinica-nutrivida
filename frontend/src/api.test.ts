import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AccountInfo } from '@azure/msal-browser';

const msalMock = vi.hoisted(() => ({
  getActiveAccount: vi.fn(),
  getAllAccounts: vi.fn(),
  acquireTokenSilent: vi.fn(),
  acquireTokenPopup: vi.fn()
}));

vi.mock('./auth', () => ({ msalInstance: msalMock }));

import { apiUrl, clinicApi, type Reserva } from './api';
import { roleFor, roleOf } from './roles';

const account = { username: 'paciente@gmail.com', homeAccountId: '1', environment: '', tenantId: '', localAccountId: '' } as AccountInfo;

describe('clinicApi', () => {
  beforeEach(() => {
    msalMock.getActiveAccount.mockReturnValue(null);
    msalMock.getAllAccounts.mockReturnValue([]);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

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
    expect(url).toBe(`${apiUrl}/api/reservas`);
    expect(options.method).toBe('POST');
    expect(options.body).toBe(JSON.stringify(reserva));
    expect(new Headers(options.headers).has('Authorization')).toBe(false);
  });

  it('attaches the Azure AD access token as a Bearer header', async () => {
    msalMock.getActiveAccount.mockReturnValue(account);
    msalMock.acquireTokenSilent.mockResolvedValue({ accessToken: 'jwt-de-prueba' });
    const fetchMock = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    await clinicApi.fichas();

    const [, options] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer jwt-de-prueba');
    expect(msalMock.acquireTokenSilent).toHaveBeenCalledWith(expect.objectContaining({
      account,
      scopes: [expect.stringMatching(/^api:\/\/.+\/access_as_user$/)]
    }));
  });

  it('reports unsuccessful API responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 503, statusText: 'Unavailable' })));

    await expect(clinicApi.reservas()).rejects.toThrow('Error 503: Unavailable');
  });

  it('reports authorization errors from the backend', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 403 })));

    await expect(clinicApi.reservas()).rejects.toThrow('Error 403');
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
    expect(fetchMock.mock.calls[0]?.[0]).toBe(`${apiUrl}/api/nutricionistas`);
    expect(fetchMock.mock.calls[1]?.[0]).toBe(`${apiUrl}/api/nutricionistas/9`);
  });
});

describe('roles', () => {
  it('maps known emails to their role', () => {
    expect(roleFor('nicolasxd08@gmail.com')).toBe('Administrador');
    expect(roleFor('LUIS.CONCHA.D5@gmail.com')).toBe('Secretaria');
    expect(roleFor('otro@gmail.com')).toBe('Paciente');
    expect(roleFor('')).toBe('');
  });

  it('prefers the Azure AD roles claim over the email', () => {
    expect(roleOf({ ...account, idTokenClaims: { roles: ['Nutricionista'] } })).toBe('Nutricionista');
    expect(roleOf(account)).toBe('Paciente');
    expect(roleOf(null)).toBe('');
  });
});
