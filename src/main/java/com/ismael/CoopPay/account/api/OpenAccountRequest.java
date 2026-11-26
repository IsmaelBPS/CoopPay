package com.ismael.CoopPay.account.api;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@NotNull(message = "associateId é obrigatório") UUID associateId) {
}
