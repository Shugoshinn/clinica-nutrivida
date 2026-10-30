import type { ReactNode } from 'react';
import { InteractionStatus } from '@azure/msal-browser';
import { useIsAuthenticated, useMsal } from '@azure/msal-react';
import { Navigate } from 'react-router-dom';
import { roleOf, type Role } from './roles';

interface ProtectedRouteProps {
  roles?: Role[];
  children: ReactNode;
}

// Ruta protegida: exige sesión de Azure AD y, opcionalmente, uno de los roles indicados
export function ProtectedRoute({ roles, children }: ProtectedRouteProps) {
  const { accounts, inProgress } = useMsal();
  const isAuthenticated = useIsAuthenticated();

  if (inProgress !== InteractionStatus.None) return null;
  if (!isAuthenticated) return <Navigate to="/iniciar-sesion" replace />;
  if (roles && !roles.includes(roleOf(accounts[0]))) return <Navigate to="/" replace />;

  return <>{children}</>;
}
