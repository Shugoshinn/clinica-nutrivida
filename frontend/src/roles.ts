import type { AccountInfo } from '@azure/msal-browser';

export type Role = 'Paciente' | 'Nutricionista' | 'Secretaria' | 'Administrador' | '';

const knownRoles: Role[] = ['Administrador', 'Secretaria', 'Nutricionista', 'Paciente'];

// Respaldo para cuentas personales de Microsoft sin App Roles (mismo mapeo que app.security.role-mappings del backend)
export function roleFor(email: string): Role {
  const normalizedEmail = email.toLowerCase();
  if (normalizedEmail === 'becerrajohan34@gmail.com') return 'Nutricionista';
  if (normalizedEmail === 'luis.concha.d5@gmail.com') return 'Secretaria';
  if (normalizedEmail === 'nicolasxd08@gmail.com') return 'Administrador';
  return email ? 'Paciente' : '';
}

// El rol se toma primero del claim "roles" del token de Azure y, si no viene, del correo
export function roleOf(account: AccountInfo | null | undefined): Role {
  if (!account) return '';
  const claimRoles = account.idTokenClaims?.roles ?? [];
  const claimRole = knownRoles.find((role) => claimRoles.includes(role));
  return claimRole ?? roleFor(account.username ?? '');
}
