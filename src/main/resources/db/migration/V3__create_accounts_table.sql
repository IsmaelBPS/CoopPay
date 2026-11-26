CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    associate_id UUID NOT NULL UNIQUE REFERENCES associates (id),
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
