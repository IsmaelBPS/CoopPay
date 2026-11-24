package com.ismael.CoopPay.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.ledger.domain.LedgerRepository;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @Test
    void recordEntry_shouldSaveAndReturnNewEntry_whenIdempotencyKeyDoesNotExist() {
        UUID generatedId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-23T14:00:00Z");
        BigDecimal amount = new BigDecimal("0.01");
        BigDecimal balanceAfter = BigDecimal.ZERO;
        String idempotencyKey = "idem-123";
        String description = "Saque mínimo zerando saldo";

        LedgerEntry savedEntry = new LedgerEntry(
                generatedId,
                transactionId,
                accountId,
                LedgerEntryType.DEBIT,
                LedgerOperationType.WITHDRAWAL,
                amount,
                balanceAfter,
                idempotencyKey,
                description,
                createdAt
        );

        when(ledgerRepository.findByIdempotencyKeyAndAccountIdAndEntryType(
                idempotencyKey, accountId, LedgerEntryType.DEBIT
        )).thenReturn(Optional.empty());
        when(ledgerRepository.save(any(LedgerEntry.class))).thenReturn(savedEntry);

        LedgerEntry result = ledgerService.recordEntry(
                transactionId,
                accountId,
                LedgerEntryType.DEBIT,
                LedgerOperationType.WITHDRAWAL,
                amount,
                balanceAfter,
                idempotencyKey,
                description
        );

        assertThat(result).isSameAs(savedEntry);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());
        LedgerEntry captured = captor.getValue();
        assertThat(captured.getId()).isNull();
        assertThat(captured.getTransactionId()).isEqualTo(transactionId);
        assertThat(captured.getAccountId()).isEqualTo(accountId);
        assertThat(captured.getEntryType()).isEqualTo(LedgerEntryType.DEBIT);
        assertThat(captured.getOperationType()).isEqualTo(LedgerOperationType.WITHDRAWAL);
        assertThat(captured.getAmount()).isEqualByComparingTo("0.01");
        assertThat(captured.getBalanceAfter()).isEqualByComparingTo("0.00");
        assertThat(captured.getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(captured.getDescription()).isEqualTo(description);
        assertThat(captured.getCreatedAt()).isNull();
    }

    @Test
    void recordEntry_shouldReturnExistingEntry_whenIdempotencyKeyAlreadyExists() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        String idempotencyKey = "idem-existing";
        LedgerEntry existing = new LedgerEntry(
                UUID.randomUUID(),
                transactionId,
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                idempotencyKey,
                "Depósito",
                Instant.now()
        );

        when(ledgerRepository.findByIdempotencyKeyAndAccountIdAndEntryType(
                idempotencyKey, accountId, LedgerEntryType.CREDIT
        )).thenReturn(Optional.of(existing));

        LedgerEntry result = ledgerService.recordEntry(
                transactionId,
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("150.00"),
                new BigDecimal("150.00"),
                idempotencyKey,
                "Depósito"
        );

        assertThat(result).isSameAs(existing);
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void recordEntry_shouldThrow_whenTransactionIdIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                null,
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction ID é obrigatório");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenAccountIdIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                null,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Account ID é obrigatório");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenEntryTypeIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tipo de lançamento é obrigatório");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenOperationTypeIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                null,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tipo de operação é obrigatório");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenAmountIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                null,
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valor do lançamento deve ser maior que zero");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenAmountIsZero() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                BigDecimal.ZERO,
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valor do lançamento deve ser maior que zero");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenAmountIsNegative() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("-0.01"),
                new BigDecimal("10.00"),
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valor do lançamento deve ser maior que zero");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenBalanceAfterIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                null,
                "idem-1",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Saldo resultante é obrigatório");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenIdempotencyKeyIsNull() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                null,
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chave de idempotência é obrigatória");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void recordEntry_shouldThrow_whenIdempotencyKeyIsBlank() {
        assertThatThrownBy(() -> ledgerService.recordEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "   ",
                "desc"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Chave de idempotência é obrigatória");

        verifyNoInteractions(ledgerRepository);
    }

    @Test
    void findById_shouldReturnEntry_whenFound() {
        UUID id = UUID.randomUUID();
        LedgerEntry expected = new LedgerEntry(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                "idem-find",
                "Depósito",
                Instant.now()
        );
        when(ledgerRepository.findById(id)).thenReturn(Optional.of(expected));

        LedgerEntry result = ledgerService.findById(id);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(ledgerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ledgerService.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lançamento não encontrado: " + id);
    }

    @Test
    void findByAccountId_shouldReturnEntriesFromRepository() {
        UUID accountId = UUID.randomUUID();
        LedgerEntry entry = new LedgerEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                "idem-acc",
                "Depósito",
                Instant.now()
        );
        when(ledgerRepository.findByAccountId(accountId)).thenReturn(List.of(entry));

        List<LedgerEntry> result = ledgerService.findByAccountId(accountId);

        assertThat(result).containsExactly(entry);
    }

    @Test
    void findByAccountId_shouldReturnEmptyList_whenNoEntriesExist() {
        UUID accountId = UUID.randomUUID();
        when(ledgerRepository.findByAccountId(accountId)).thenReturn(List.of());

        List<LedgerEntry> result = ledgerService.findByAccountId(accountId);

        assertThat(result).isEmpty();
    }

    @Test
    void findByTransactionId_shouldReturnEntriesFromRepository() {
        UUID transactionId = UUID.randomUUID();
        LedgerEntry debit = new LedgerEntry(
                UUID.randomUUID(),
                transactionId,
                UUID.randomUUID(),
                LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("75.00"),
                new BigDecimal("25.00"),
                "idem-tx",
                "Enviada",
                Instant.now()
        );
        when(ledgerRepository.findByTransactionId(transactionId)).thenReturn(List.of(debit));

        List<LedgerEntry> result = ledgerService.findByTransactionId(transactionId);

        assertThat(result).containsExactly(debit);
    }

    @Test
    void findByTransactionId_shouldReturnEmptyList_whenNoEntriesExist() {
        UUID transactionId = UUID.randomUUID();
        when(ledgerRepository.findByTransactionId(transactionId)).thenReturn(List.of());

        List<LedgerEntry> result = ledgerService.findByTransactionId(transactionId);

        assertThat(result).isEmpty();
    }
}
