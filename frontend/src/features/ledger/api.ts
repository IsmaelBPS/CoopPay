import { apiRequest } from '../../shared/api/http';

export type LedgerEntryType = 'CREDIT' | 'DEBIT';
export type LedgerOperationType = string;

export type LedgerEntryResponse = {
  id: string;
  transactionId: string;
  accountId: string;
  entryType: LedgerEntryType;
  operationType: LedgerOperationType;
  amount: number;
  balanceAfter: number;
  idempotencyKey: string;
  description: string | null;
  createdAt: string;
};

export function getLedgerByAccount(accountId: string): Promise<LedgerEntryResponse[]> {
  return apiRequest(`/ledger/accounts/${encodeURIComponent(accountId)}`);
}

export function getLedgerByTransaction(transactionId: string): Promise<LedgerEntryResponse[]> {
  return apiRequest(`/ledger/transactions/${encodeURIComponent(transactionId)}`);
}
