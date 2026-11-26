package com.ismael.CoopPay;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

/** Massa mínima de teste: ledger_entries exige conta real (FK). */
public final class TestData {

    private TestData() {
    }

    public static UUID createAccount(JdbcTemplate jdbc) {
        UUID associateId = UUID.randomUUID();
        jdbc.update("INSERT INTO associates (id, name, email, password, type) VALUES (?, 'Teste', ?, 'x', 'INDIVIDUAL')",
                associateId, associateId + "@cooppay.com");
        UUID accountId = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, associate_id, balance, status, created_at, updated_at) VALUES (?, ?, 0, 'ACTIVE', ?, ?)",
                accountId, associateId, java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now()));
        return accountId;
    }
}
