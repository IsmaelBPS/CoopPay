package com.ismael.CoopPay.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.domain.AccountRepository;
import com.ismael.CoopPay.account.domain.AccountStatus;
import com.ismael.CoopPay.associate.AssociateService;
import com.ismael.CoopPay.ledger.LedgerService;
import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.shared.exception.AccountNotActiveException;
import com.ismael.CoopPay.shared.exception.InsufficientFundsException;
import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AssociateService associateService;
    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private AccountService service;

    private Account account(UUID id, String balance, AccountStatus status) {
        Instant now = Instant.now();
        return new Account(id, UUID.randomUUID(), new BigDecimal(balance), status, now, now);
    }

    @Test
    void open_shouldRejectSecondAccountForSameAssociate() {
        UUID associateId = UUID.randomUUID();
        when(accountRepository.existsByAssociateId(associateId)).thenReturn(true);

        assertThatThrownBy(() -> service.open(associateId)).isInstanceOf(ResourceConflictException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_shouldCreditAndRecordLedger() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account(id, "10.00", AccountStatus.ACTIVE)));
        when(ledgerService.findReplay(eq("k"), eq(id), eq(LedgerEntryType.CREDIT), eq(LedgerOperationType.DEPOSIT), any()))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Account result = service.deposit(id, new BigDecimal("5.50"), "k");

        assertThat(result.getBalance()).isEqualByComparingTo("15.50");
        verify(ledgerService).recordEntry(any(), eq(id), eq(LedgerEntryType.CREDIT), eq(LedgerOperationType.DEPOSIT),
                eq(new BigDecimal("5.50")), eq(new BigDecimal("15.50")), eq("k"), any());
    }

    @Test
    void deposit_shouldNotCreditAgain_whenIdempotencyKeyReplayed() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account(id, "10.00", AccountStatus.ACTIVE)));
        when(ledgerService.findReplay(any(), any(), any(), any(), any())).thenReturn(Optional.of(org.mockito.Mockito.mock(LedgerEntry.class)));

        Account result = service.deposit(id, new BigDecimal("5.00"), "k");

        assertThat(result.getBalance()).isEqualByComparingTo("10.00");
        verify(accountRepository, never()).save(any());
        verify(ledgerService, never()).recordEntry(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void withdraw_shouldFail_whenInsufficientFunds() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account(id, "10.00", AccountStatus.ACTIVE)));
        when(ledgerService.findReplay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdraw(id, new BigDecimal("10.01"), "k"))
                .isInstanceOf(InsufficientFundsException.class);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_shouldFail_whenAccountBlocked() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account(id, "0.00", AccountStatus.BLOCKED)));
        when(ledgerService.findReplay(any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deposit(id, BigDecimal.ONE, "k")).isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void transfer_shouldRejectSameAccount() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.transfer(id, id, BigDecimal.ONE, "k")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void movements_shouldRejectMissingOrBlankIdempotencyKey() {
        UUID id = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        assertThatThrownBy(() -> service.deposit(id, BigDecimal.ONE, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.withdraw(id, BigDecimal.ONE, " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.transfer(id, targetId, BigDecimal.ONE, "")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(accountRepository, ledgerService);
    }

    @Test
    void operations_shouldRejectInvalidAmounts() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.deposit(id, BigDecimal.ZERO, "k")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.withdraw(id, new BigDecimal("1.001"), "k")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById_shouldThrow_whenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
