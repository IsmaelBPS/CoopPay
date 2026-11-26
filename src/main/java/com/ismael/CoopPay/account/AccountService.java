package com.ismael.CoopPay.account;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.domain.AccountRepository;
import com.ismael.CoopPay.associate.AssociateService;
import com.ismael.CoopPay.ledger.LedgerService;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final MeterRegistry meterRegistry;
    private final AssociateService associateService;
    private final LedgerService ledgerService;

    @Transactional
    public Account open(UUID associateId) {
        associateService.findById(associateId);
        if (accountRepository.existsByAssociateId(associateId)) {
            throw new ResourceConflictException("Associado já possui conta: " + associateId);
        }
        Account saved = accountRepository.save(Account.open(associateId));
        meterRegistry.counter("cooppay.accounts.operations", "action", "opened", "outcome", "success").increment();
        log.info("Conta aberta: accountId={}, associateId={}", saved.getId(), associateId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public Account deposit(UUID accountId, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        String key = keyOrRandom(idempotencyKey);
        Account account = lock(accountId);
        // Retry com a mesma chave não pode creditar duas vezes.
        if (ledgerService.findReplay(key, accountId, LedgerEntryType.CREDIT, LedgerOperationType.DEPOSIT, amount).isPresent()) {
            return account;
        }
        account.credit(amount);
        Account saved = accountRepository.save(account);
        ledgerService.recordEntry(UUID.randomUUID(), accountId, LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT, amount, saved.getBalance(), key, "Depósito");
        return saved;
    }

    @Transactional
    public Account withdraw(UUID accountId, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        String key = keyOrRandom(idempotencyKey);
        Account account = lock(accountId);
        if (ledgerService.findReplay(key, accountId, LedgerEntryType.DEBIT, LedgerOperationType.WITHDRAWAL, amount).isPresent()) {
            return account;
        }
        account.debit(amount);
        Account saved = accountRepository.save(account);
        ledgerService.recordEntry(UUID.randomUUID(), accountId, LedgerEntryType.DEBIT,
                LedgerOperationType.WITHDRAWAL, amount, saved.getBalance(), key, "Saque");
        return saved;
    }

    /** Retorna a conta de origem após a transferência. */
    @Transactional
    public Account transfer(UUID sourceId, UUID targetId, BigDecimal amount, String idempotencyKey) {
        validateAmount(amount);
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
        }
        String key = keyOrRandom(idempotencyKey);
        // Ordem determinística de locks evita deadlock em transferências cruzadas (A→B e B→A).
        boolean sourceFirst = sourceId.compareTo(targetId) < 0;
        Account first = lock(sourceFirst ? sourceId : targetId);
        Account second = lock(sourceFirst ? targetId : sourceId);
        Account source = sourceFirst ? first : second;
        Account target = sourceFirst ? second : first;

        if (ledgerService.findReplay(key, sourceId, LedgerEntryType.DEBIT, LedgerOperationType.TRANSFER, amount).isPresent()) {
            return source;
        }
        source.debit(amount);
        target.credit(amount);
        Account savedSource = accountRepository.save(source);
        Account savedTarget = accountRepository.save(target);
        UUID transactionId = UUID.randomUUID();
        ledgerService.recordEntry(transactionId, sourceId, LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER, amount, savedSource.getBalance(), key, "Transferência enviada");
        ledgerService.recordEntry(transactionId, targetId, LedgerEntryType.CREDIT,
                LedgerOperationType.TRANSFER, amount, savedTarget.getBalance(), key, "Transferência recebida");
        return savedSource;
    }

    private Account lock(UUID id) {
        return accountRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    }

    private ResourceNotFoundException notFound(UUID id) {
        return new ResourceNotFoundException("Conta não encontrada: " + id);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior que zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Valor deve ter no máximo 2 casas decimais");
        }
    }

    private String keyOrRandom(String key) {
        return key == null || key.isBlank() ? UUID.randomUUID().toString() : key;
    }
}
