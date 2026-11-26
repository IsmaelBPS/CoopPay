package com.ismael.CoopPay.account.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.domain.AccountStatus;

public record AccountResponse(
        UUID id,
        UUID associateId,
        BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAssociateId(), a.getBalance(), a.getStatus(),
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
