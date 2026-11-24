package com.ismael.CoopPay.ledger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.ledger.domain.LedgerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    @Transactional
    public LedgerEntry recordEntry(
            UUID transactionId,
            UUID accountId,
            LedgerEntryType entryType,
            LedgerOperationType operationType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String idempotencyKey,
            String description
    ) {
        validateEntryInput(transactionId, accountId, entryType, operationType, amount, balanceAfter, idempotencyKey);

        Optional<LedgerEntry> existing = ledgerRepository.findByIdempotencyKeyAndAccountIdAndEntryType(
                idempotencyKey,
                accountId,
                entryType
        );
        if (existing.isPresent()) {
            log.info("Lançamento idempotente reutilizado: entryId={}, transactionId={}, accountId={}, entryType={}",
                    existing.get().getId(), transactionId, accountId, entryType);
            return existing.get();
        }

        LedgerEntry entry = new LedgerEntry(
                null,
                transactionId,
                accountId,
                entryType,
                operationType,
                amount,
                balanceAfter,
                idempotencyKey,
                description,
                null
        );
        LedgerEntry saved = ledgerRepository.save(entry);
        log.info("Lançamento registrado: entryId={}, transactionId={}, accountId={}, entryType={}, operationType={}, amount={}",
                saved.getId(), transactionId, accountId, entryType, operationType, amount);
        return saved;
    }

    @Transactional(readOnly = true)
    public LedgerEntry findById(UUID id) {
        return ledgerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findByAccountId(UUID accountId) {
        return ledgerRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findByTransactionId(UUID transactionId) {
        return ledgerRepository.findByTransactionId(transactionId);
    }

    private void validateEntryInput(
            UUID transactionId,
            UUID accountId,
            LedgerEntryType entryType,
            LedgerOperationType operationType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String idempotencyKey
    ) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID é obrigatório");
        }
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID é obrigatório");
        }
        if (entryType == null) {
            throw new IllegalArgumentException("Tipo de lançamento é obrigatório");
        }
        if (operationType == null) {
            throw new IllegalArgumentException("Tipo de operação é obrigatório");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do lançamento deve ser maior que zero");
        }
        if (balanceAfter == null) {
            throw new IllegalArgumentException("Saldo resultante é obrigatório");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Chave de idempotência é obrigatória");
        }
    }
}
