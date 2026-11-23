CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL,
    account_id UUID NOT NULL,
    entry_type VARCHAR(20) NOT NULL,
    operation_type VARCHAR(30) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    balance_after NUMERIC(19, 2) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_ledger_idempotency_account_entry UNIQUE (idempotency_key, account_id, entry_type)
);

CREATE INDEX idx_ledger_entries_account_created_at ON ledger_entries (account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
