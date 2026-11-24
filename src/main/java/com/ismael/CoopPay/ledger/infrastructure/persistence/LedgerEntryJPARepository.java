package com.ismael.CoopPay.ledger.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ismael.CoopPay.ledger.domain.LedgerEntryType;

public interface LedgerEntryJPARepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<LedgerEntryEntity> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);

    Optional<LedgerEntryEntity> findByIdempotencyKeyAndAccountIdAndEntryType(
            String idempotencyKey,
            UUID accountId,
            LedgerEntryType entryType
    );
}
