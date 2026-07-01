import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';
import type { Role } from '../types';

/**
 * Guards nested routes. Redirects to /login when unauthenticated, or to the
 * user's home when authenticated but lacking one of the allowed roles.
 */
export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="centered">Loading…</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (roles && roles.length > 0 && !roles.includes(user.role)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
