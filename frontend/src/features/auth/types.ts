export type AuthMode = 'api' | 'preview';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

export interface LoginResult {
  token: string;
  user: AuthUser;
  mode: AuthMode;
}
