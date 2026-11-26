import { apiRequest } from '../../shared/api/http';

export interface AccountResponse {
  id: string;
  associateId: string;
  balance: number;
  status: 'ACTIVE' | 'BLOCKED' | 'CLOSED';
  createdAt: string;
  updatedAt: string;
}

const post = <T>(path: string, body: unknown, idempotencyKey?: string) =>
  apiRequest<T>(path, {
    method: 'POST',
    body: JSON.stringify(body),
    headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
  });

export const getAccount = (id: string) => apiRequest<AccountResponse>(`/accounts/${encodeURIComponent(id)}`);

export const openAccount = (associateId: string) => post<AccountResponse>('/accounts', { associateId });

export const deposit = (id: string, amount: number, key: string) =>
  post<AccountResponse>(`/accounts/${encodeURIComponent(id)}/deposit`, { amount }, key);

export const withdraw = (id: string, amount: number, key: string) =>
  post<AccountResponse>(`/accounts/${encodeURIComponent(id)}/withdraw`, { amount }, key);

export const transfer = (id: string, targetAccountId: string, amount: number, key: string) =>
  post<AccountResponse>(`/accounts/${encodeURIComponent(id)}/transfers`, { targetAccountId, amount }, key);
