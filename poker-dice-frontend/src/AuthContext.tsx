/*import React, { createContext, useContext, useState, useMemo } from "react";
import { User } from "./types";
import { api } from "./api";
import { useFetch } from "./hooks/useFetch";

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string) => Promise<void>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem("token")
  );

  const authHeaders = useMemo(() => getAuthHeaders(), [token]);
  const userState = useFetch<User>(token ? "/me" : undefined, {
    headers: authHeaders,
  });

  // Handle token removal on error
  React.useEffect(() => {
    if (userState.type === "error" && token) {
      localStorage.removeItem("token");
      setToken(null);
    }
  }, [userState.type, token]);

  const login = async (newToken: string) => {
    localStorage.setItem("token", newToken);
    setToken(newToken);
  };

  const logout = () => {
    api.logout();
    localStorage.removeItem("token");
    setToken(null);
  };

  const user = userState.type === "loaded" ? userState.payload : null;
  const isLoading = userState.type === "loading";

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}

 */


import React, { createContext, useContext, useState } from "react";
import { User } from "./types";
import { api } from "./api";
import { useFetch } from "./hooks/useFetch";

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    login: () => void;
    logout: () => void;
    isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
        // Verificar se o cookie existe
        return document.cookie.includes("auth-token");
    });

    const userState = useFetch<User>(isAuthenticated ? "/me" : undefined);

    // Handle authentication removal on error
    React.useEffect(() => {
        if (userState.type === "error" && isAuthenticated) {
            setIsAuthenticated(false);
        }
    }, [userState.type, isAuthenticated]);

    const login = () => {
        // Após login bem-sucedido, o cookie é criado automaticamente
        setIsAuthenticated(true);
    };

    const logout = async () => {
        await api.logout(); // Remove o cookie no backend
        setIsAuthenticated(false);
    };

    const user = userState.type === "loaded" ? userState.payload : null;
    const isLoading = userState.type === "loading";

    return (
        <AuthContext.Provider value={{ user, isAuthenticated, login, logout, isLoading }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within AuthProvider");
    }
    return context;
}

