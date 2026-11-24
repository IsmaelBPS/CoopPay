package com.ismael.CoopPay.ledger.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final LedgerEntryJPARepository jpaRepository;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryEntity saved = jpaRepository.save(LedgerEntryEntity.fromDomain(entry));
        return saved.toDomain();
    }

    @Override
    public Optional<LedgerEntry> findById(UUID id) {
        return jpaRepository.findById(id).map(LedgerEntryEntity::toDomain);
    }

    @Override
    public List<LedgerEntry> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(LedgerEntryEntity::toDomain)
                .toList();
    }

    @Override
    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return jpaRepository.findByTransactionIdOrderByCreatedAtAsc(transactionId)
                .stream()
                .map(LedgerEntryEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<LedgerEntry> findByIdempotencyKeyAndAccountIdAndEntryType(
            String idempotencyKey,
            UUID accountId,
            LedgerEntryType entryType
    ) {
        return jpaRepository.findByIdempotencyKeyAndAccountIdAndEntryType(idempotencyKey, accountId, entryType)
                .map(LedgerEntryEntity::toDomain);
    }
}
