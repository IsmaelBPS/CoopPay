package com.ismael.CoopPay.ledger.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerRepository {

    LedgerEntry save(LedgerEntry entry);

    Optional<LedgerEntry> findById(UUID id);

    List<LedgerEntry> findByAccountId(UUID accountId);

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Optional<LedgerEntry> findByIdempotencyKeyAndAccountIdAndEntryType(
            String idempotencyKey,
            UUID accountId,
            LedgerEntryType entryType
    );
}
