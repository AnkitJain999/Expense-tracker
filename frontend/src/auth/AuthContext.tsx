import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { fetchMe, login as loginRequest } from '../api/auth';
import { getToken, setToken } from '../api/client';
import type { Role, UserProfile } from '../types';

interface AuthContextValue {
  user: UserProfile | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<UserProfile>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  // On mount, if a token exists, resolve the current user.
  useEffect(() => {
    let active = true;
    async function bootstrap() {
      if (!getToken()) {
        setLoading(false);
        return;
      }
      try {
        const profile = await fetchMe();
        if (active) setUser(profile);
      } catch {
        setToken(null);
      } finally {
        if (active) setLoading(false);
      }
    }
    void bootstrap();
    return () => {
      active = false;
    };
  }, []);

  async function login(email: string, password: string): Promise<UserProfile> {
    const response = await loginRequest(email, password);
    setToken(response.token);
    setUser(response.user);
    return response.user;
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      login,
      logout,
      hasRole: (...roles: Role[]) => (user ? roles.includes(user.role) : false),
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
