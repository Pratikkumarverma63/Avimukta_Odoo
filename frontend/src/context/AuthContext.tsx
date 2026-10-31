import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { authApi } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  isAdmin: boolean;
  isStaff: boolean;
  isCustomer: boolean;
  login: (credentials: { email: string; password: string }) => Promise<User>;
  register: (data: { email: string; password: string; firstName: string; lastName: string; companyName?: string }) => Promise<User>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const verifyUser = async () => {
      const savedToken = localStorage.getItem('token');
      const savedUser = localStorage.getItem('user');
      if (savedToken && savedUser) {
        try {
          const res = await authApi.getCurrentUser();
          const p = res.data;
          const u: User = {
            id: p.id,
            email: p.email,
            firstName: p.firstName,
            lastName: p.lastName,
            role: p.role as Role,
            active: p.active,
            customerId: p.customerId,
            customerCode: p.customerCode,
            companyName: p.companyName,
          };
          setUser(u);
          localStorage.setItem('user', JSON.stringify(u));
        } catch {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          setUser(null);
          setToken(null);
        }
      }
      setLoading(false);
    };

    verifyUser();
  }, []);

  const login = async (credentials: { email: string; password: string }): Promise<User> => {
    const res = await authApi.login(credentials);
    const authData = res.data;
    const userData: User = {
      id: authData.userId,
      email: authData.email,
      firstName: authData.firstName,
      lastName: authData.lastName,
      role: authData.role,
      active: true,
      customerId: authData.customerId,
      customerCode: authData.customerCode,
      companyName: authData.companyName,
    };
    setToken(authData.token);
    setUser(userData);
    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(userData));
    return userData;
  };

  const register = async (data: { email: string; password: string; firstName: string; lastName: string; companyName?: string }): Promise<User> => {
    const res = await authApi.register(data);
    const authData = res.data;
    const userData: User = {
      id: authData.userId,
      email: authData.email,
      firstName: authData.firstName,
      lastName: authData.lastName,
      role: authData.role,
      active: true,
      customerId: authData.customerId,
      customerCode: authData.customerCode,
      companyName: authData.companyName,
    };
    setToken(authData.token);
    setUser(userData);
    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(userData));
    return userData;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  };

  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isStaff = user?.role === 'ROLE_STAFF';
  const isCustomer = user?.role === 'ROLE_CUSTOMER';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        isAdmin,
        isStaff,
        isCustomer,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
