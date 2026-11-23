export type AssociateType = 'INDIVIDUAL' | 'COMPANY';

export interface AssociateResponse {
  id: string;
  name: string;
  email: string;
  type: AssociateType;
}

export interface CreateAssociateRequest {
  name: string;
  email: string;
  password: string;
  type: AssociateType;
}

export interface UpdateAssociateRequest {
  name: string;
  email: string;
  type: AssociateType;
}
