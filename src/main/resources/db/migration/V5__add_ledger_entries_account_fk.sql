ALTER TABLE ledger_entries
    ADD CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_id) REFERENCES accounts (id);
