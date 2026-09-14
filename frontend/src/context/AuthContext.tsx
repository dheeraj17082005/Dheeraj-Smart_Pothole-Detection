import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role, VerificationStatus, AuthResponse } from '../types';
import { apiClient } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  verificationStatus: VerificationStatus | null;
  isAuthenticated: boolean;
  isCitizen: boolean;
  isOfficer: boolean;
  isVerifiedOfficer: boolean;
  initialLoading: boolean;
  login: (data: { email: string; password: string }) => Promise<AuthResponse>;
  logout: () => void;
  setAuthData: (response: AuthResponse) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Use sessionStorage so new browser opens always start on the login page for clean testing
  const [token, setToken] = useState<string | null>(() => {
    return sessionStorage.getItem('authToken');
  });
  const [user, setUser] = useState<User | null>(() => {
    const savedUser = sessionStorage.getItem('authUser');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [verificationStatus, setVerificationStatus] = useState<VerificationStatus | null>(() => {
    return (sessionStorage.getItem('authVerificationStatus') as VerificationStatus) || null;
  });
  const [initialLoading, setInitialLoading] = useState<boolean>(true);

  useEffect(() => {
    // Clear any legacy localStorage keys to ensure fresh browser window opens start at login
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
    localStorage.removeItem('authVerificationStatus');

    if (token && !user) {
      setInitialLoading(true);
      apiClient.getCurrentUser()
        .then((fetchedUser) => {
          setUser(fetchedUser);
          sessionStorage.setItem('authUser', JSON.stringify(fetchedUser));
        })
        .catch(() => {
          logout();
        })
        .finally(() => {
          setInitialLoading(false);
        });
    } else {
      setInitialLoading(false);
    }
  }, [token]);

  const setAuthData = (response: AuthResponse) => {
    setToken(response.token);
    sessionStorage.setItem('authToken', response.token);

    const userObj: User = {
      id: response.id,
      email: response.email,
      fullName: response.fullName,
      role: response.role
    };
    setUser(userObj);
    sessionStorage.setItem('authUser', JSON.stringify(userObj));

    if (response.verificationStatus) {
      setVerificationStatus(response.verificationStatus);
      sessionStorage.setItem('authVerificationStatus', response.verificationStatus);
    } else {
      setVerificationStatus(null);
      sessionStorage.removeItem('authVerificationStatus');
    }
  };

  const login = async (data: { email: string; password: string }) => {
    const response = await apiClient.login(data);
    setAuthData(response);
    return response;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    setVerificationStatus(null);
    sessionStorage.removeItem('authToken');
    sessionStorage.removeItem('authUser');
    sessionStorage.removeItem('authVerificationStatus');
    localStorage.removeItem('authToken');
    localStorage.removeItem('authUser');
    localStorage.removeItem('authVerificationStatus');
  };

  const isAuthenticated = !!token && !!user;
  const isCitizen = user?.role === 'ROLE_USER';
  const isOfficer = user?.role === 'ROLE_OFFICER';
  const isVerifiedOfficer = isOfficer && verificationStatus === 'VERIFIED';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        verificationStatus,
        isAuthenticated,
        isCitizen,
        isOfficer,
        isVerifiedOfficer,
        initialLoading,
        login,
        logout,
        setAuthData
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
