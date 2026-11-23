package com.ismael.CoopPay.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LedgerEntry {
    private final UUID id;
    private final UUID transactionId;
    private final UUID accountId;
    private final LedgerEntryType entryType;
    private final LedgerOperationType operationType;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String idempotencyKey;
    private final String description;
    private final Instant createdAt;
}
