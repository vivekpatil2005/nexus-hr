import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import api from './api';

interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  roles: string[];
  enabled: boolean;
  employeeId?: number | null;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  signup: (username: string, email: string, fullName: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('accessToken'));
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (token) {
      fetchUser();
    } else {
      setIsLoading(false);
    }
  }, []);

  const fetchUser = async () => {
    try {
      const { data } = await api.get('/auth/me');
      setUser(data.data);
    } catch {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      setToken(null);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (usernameOrEmail: string, password: string) => {
    const { data } = await api.post('/auth/login', { usernameOrEmail, password });
    const resp = data.data;
    localStorage.setItem('accessToken', resp.accessToken);
    localStorage.setItem('refreshToken', resp.refreshToken);
    setToken(resp.accessToken);
    const meResp = await api.get('/auth/me', {
      headers: { Authorization: `Bearer ${resp.accessToken}` },
    });
    setUser(meResp.data.data);
  };

  const signup = async (username: string, email: string, fullName: string, password: string) => {
    const { data } = await api.post('/auth/signup', { username, email, fullName, password });
    const resp = data.data;
    localStorage.setItem('accessToken', resp.accessToken);
    localStorage.setItem('refreshToken', resp.refreshToken);
    setToken(resp.accessToken);
    const meResp = await api.get('/auth/me', {
      headers: { Authorization: `Bearer ${resp.accessToken}` },
    });
    setUser(meResp.data.data);
  };

  const logout = () => {
    api.post('/auth/logout').catch(() => {});
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setToken(null);
    setUser(null);
  };

  const hasRole = (role: string) => {
    if (!user?.roles) return false;
    const cleanRole = role.startsWith('ROLE_') ? role.substring(5) : role;
    return user.roles.some(r => {
      const cleanR = r.startsWith('ROLE_') ? r.substring(5) : r;
      return cleanR === cleanRole;
    });
  };

  return (
    <AuthContext.Provider
      value={{ user, token, isAuthenticated: !!user, isLoading, login, signup, logout, hasRole }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
