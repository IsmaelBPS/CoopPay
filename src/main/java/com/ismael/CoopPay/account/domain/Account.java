package com.ismael.CoopPay.account.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ismael.CoopPay.shared.exception.AccountNotActiveException;
import com.ismael.CoopPay.shared.exception.InsufficientFundsException;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Account {
    private final UUID id;
    private final UUID associateId;
    private BigDecimal balance;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public static Account open(UUID associateId) {
        Instant now = Instant.now();
        return new Account(null, associateId, BigDecimal.ZERO.setScale(2), AccountStatus.ACTIVE, now, now);
    }

    public void credit(BigDecimal amount) {
        requireActive();
        balance = balance.add(amount);
        updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        requireActive();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente na conta: " + id);
        }
        balance = balance.subtract(amount);
        updatedAt = Instant.now();
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Conta " + id + " não está ativa (" + status + ")");
        }
    }
}
