package com.ismael.CoopPay.account.api;

import java.math.BigDecimal;
import java.util.UUID;

import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.domain.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record BalanceResponse(
        @Schema(description = "Identificador da conta") UUID accountId,
        @Schema(description = "Saldo atual", example = "150.50") BigDecimal balance,
        @Schema(description = "Status da conta") AccountStatus status
) {
    public static BalanceResponse from(Account a) {
        return new BalanceResponse(a.getId(), a.getBalance(), a.getStatus());
    }
}
