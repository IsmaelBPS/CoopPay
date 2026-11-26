package com.ismael.CoopPay.account.api;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull(message = "targetAccountId é obrigatório") UUID targetAccountId,
        @NotNull(message = "amount é obrigatório")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero")
        @Digits(integer = 17, fraction = 2, message = "amount deve ter no máximo 2 casas decimais")
        BigDecimal amount
) {
}
