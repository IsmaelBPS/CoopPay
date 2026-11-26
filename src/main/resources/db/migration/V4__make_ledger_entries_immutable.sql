CREATE FUNCTION prevent_ledger_entries_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries é imutável: % não permitido', TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION prevent_ledger_entries_mutation();
