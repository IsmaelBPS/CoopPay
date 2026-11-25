package com.ismael.CoopPay.ledger.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ismael.CoopPay.ledger.domain.LedgerEntryType;

public interface LedgerEntryJPARepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    List<LedgerEntryEntity> findByTransactionIdOrderByCreatedAtAsc(UUID transactionId);

    Optional<LedgerEntryEntity> findByIdempotencyKeyAndAccountIdAndEntryType(
            String idempotencyKey,
            UUID accountId,
            LedgerEntryType entryType
    );

    // ON CONFLICT mantém a transação sã no Postgres (um INSERT que viola unique a abortaria).
    @Modifying
    @Query(value = """
            INSERT INTO ledger_entries (id, transaction_id, account_id, entry_type, operation_type,
                                        amount, balance_after, idempotency_key, description, created_at)
            VALUES (:id, :transactionId, :accountId, :entryType, :operationType,
                    :amount, :balanceAfter, :idempotencyKey, :description, :createdAt)
            ON CONFLICT ON CONSTRAINT uk_ledger_idempotency_account_entry DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("transactionId") UUID transactionId,
            @Param("accountId") UUID accountId,
            @Param("entryType") String entryType,
            @Param("operationType") String operationType,
            @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("description") String description,
            @Param("createdAt") Instant createdAt
    );
}
