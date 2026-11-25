package com.ismael.CoopPay.ledger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository {

    LedgerEntry save(LedgerEntry entry);

    /** Insere de forma atômica; false se a chave de idempotência já existia (inclusive em race). */
    boolean insertIfAbsent(LedgerEntry entry);

    Optional<LedgerEntry> findById(UUID id);

    List<LedgerEntry> findByAccountId(UUID accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Optional<LedgerEntry> findByIdempotencyKeyAndAccountIdAndEntryType(
            String idempotencyKey,
            UUID accountId,
            LedgerEntryType entryType
    );
}
