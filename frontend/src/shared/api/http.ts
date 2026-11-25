export interface ApiFieldError {
  field?: string;
  message?: string;
}

export class ApiError extends Error {
  status: number;
  details?: unknown;

  constructor(status: number, message: string, details?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.details = details;
  }
}

const API_BASE = '/api/v1';

async function parseErrorBody(response: Response): Promise<{ message: string; details?: unknown }> {
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
    try {
      const json = (await response.json()) as Record<string, unknown>;
      const msg =
        (typeof json.message === 'string' && json.message) ||
        (typeof json.detail === 'string' && json.detail) ||
        (typeof json.title === 'string' && json.title) ||
        (typeof json.error === 'string' && json.error) ||
        `Erro HTTP ${response.status}`;
      return { message: msg, details: json };
    } catch {
      return { message: `Erro HTTP ${response.status}` };
    }
  }
  const text = await response.text().catch(() => '');
  return { message: text.trim() || `Erro HTTP ${response.status}` };
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const url = path.startsWith('/api') ? path : `${API_BASE}${path}`;
  const headers = new Headers(init?.headers);
  if (init?.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(url, {
    ...init,
    headers,
  });

  if (!response.ok) {
    const { message, details } = await parseErrorBody(response);
    throw new ApiError(response.status, message, details);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
