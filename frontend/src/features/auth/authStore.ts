import { create } from 'zustand';
import { createJSONStorage, persist, type StateStorage } from 'zustand/middleware';
import { ApiError } from '../../shared/api/http';
import { loginWithCredentials } from './api';
import type { AuthMode, AuthUser, LoginCredentials } from './types';

const AUTH_STORAGE_KEY = 'cooppay.auth';

const authStorage: StateStorage = {
  getItem: (name) => {
    try {
      return localStorage.getItem(name) ?? sessionStorage.getItem(name);
    } catch {
      return null;
    }
  },
  setItem: (name, value) => {
    try {
      const parsed = JSON.parse(value) as { state?: { rememberMe?: boolean } };
      if (parsed?.state?.rememberMe === true) {
        localStorage.setItem(name, value);
        sessionStorage.removeItem(name);
      } else {
        sessionStorage.setItem(name, value);
        localStorage.removeItem(name);
      }
    } catch {
      // Ignore storage quota or serialization errors
    }
  },
  removeItem: (name) => {
    try {
      localStorage.removeItem(name);
      sessionStorage.removeItem(name);
    } catch {
      // Ignore storage errors
    }
  },
};

export interface AuthState {
  user: AuthUser | null;
  token: string | null;
  authMode: AuthMode | null;
  isAuthenticated: boolean;
  rememberMe: boolean;
  rememberedEmail: string;
  isLoading: boolean;
  error: string | null;
  login: (credentials: LoginCredentials) => Promise<boolean>;
  logout: () => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      authMode: null,
      isAuthenticated: false,
      rememberMe: false,
      rememberedEmail: '',
      isLoading: false,
      error: null,
      login: async (credentials) => {
        set({ isLoading: true, error: null });
        try {
          const result = await loginWithCredentials({
            email: credentials.email,
            password: credentials.password,
          });
          set({
            user: result.user,
            token: result.token,
            authMode: result.mode,
            isAuthenticated: true,
            rememberMe: credentials.rememberMe,
            rememberedEmail: credentials.rememberMe ? credentials.email : '',
            isLoading: false,
            error: null,
          });
          return true;
        } catch (err) {
          const message =
            err instanceof ApiError
              ? err.message
              : 'Não foi possível autenticar. Tente novamente.';
          set({
            isLoading: false,
            error: message,
          });
          return false;
        }
      },
      logout: () => {
        const { rememberMe, rememberedEmail } = get();
        set({
          user: null,
          token: null,
          authMode: null,
          isAuthenticated: false,
          rememberMe,
          rememberedEmail: rememberMe ? rememberedEmail : '',
          error: null,
        });
      },
      clearError: () => {
        set({ error: null });
      },
    }),
    {
      name: AUTH_STORAGE_KEY,
      storage: createJSONStorage(() => authStorage),
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        authMode: state.authMode,
        isAuthenticated: state.isAuthenticated,
        rememberMe: state.rememberMe,
        rememberedEmail: state.rememberedEmail,
      }),
    },
  ),
);
