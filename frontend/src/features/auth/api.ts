import { ApiError, apiRequest } from '../../shared/api/http';
import type { AuthResponse, LoginCredentials, LoginResult } from './types';

function derivedDisplayNameFromEmail(email: string): string {
  const localPart = email.split('@')[0] ?? 'Associado';
  const words = localPart
    .split(/[._-]+/)
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase());

  return words.length > 0 ? words.join(' ') : 'Associado CoopPay';
}

export async function loginWithCredentials({
  email,
  password,
}: Pick<LoginCredentials, 'email' | 'password'>): Promise<LoginResult> {
  try {
    const response = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });

    return {
      token: response.token,
      user: response.user,
      mode: 'api',
    };
  } catch (err) {
    if (err instanceof ApiError) {
      if (err.status === 400 || err.status === 401 || err.status === 403 || err.status === 422) {
        throw err;
      }

      if (err.status === 404 || err.status === 405) {
        return {
          token: '',
          user: {
            id: 'preview-associate',
            name: derivedDisplayNameFromEmail(email),
            email,
          },
          mode: 'preview',
        };
      }

      throw err;
    }

    if (err instanceof TypeError) {
      return {
        token: '',
        user: {
          id: 'preview-associate',
          name: derivedDisplayNameFromEmail(email),
          email,
        },
        mode: 'preview',
      };
    }

    throw err;
  }
}
