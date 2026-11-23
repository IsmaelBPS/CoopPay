import { apiRequest } from '../../shared/api/http';
import type {
  AssociateResponse,
  CreateAssociateRequest,
  UpdateAssociateRequest,
} from './types';

export function getAssociate(id: string): Promise<AssociateResponse> {
  return apiRequest<AssociateResponse>(`/associates/${encodeURIComponent(id)}`);
}

export function createAssociate(body: CreateAssociateRequest): Promise<AssociateResponse> {
  return apiRequest<AssociateResponse>('/associates', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export function updateAssociate(
  id: string,
  body: UpdateAssociateRequest,
): Promise<AssociateResponse> {
  return apiRequest<AssociateResponse>(`/associates/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

export function deleteAssociate(id: string): Promise<void> {
  return apiRequest<void>(`/associates/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}
