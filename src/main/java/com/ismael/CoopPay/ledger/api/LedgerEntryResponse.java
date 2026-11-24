package com.ismael.CoopPay.ledger.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;

import io.swagger.v3.oas.annotations.media.Schema;

public record LedgerEntryResponse(
        @Schema(description = "Identificador único do lançamento")
        UUID id,
        @Schema(description = "Identificador da transação que originou o lançamento")
        UUID transactionId,
        @Schema(description = "Identificador da conta movimentada")
        UUID accountId,
        @Schema(description = "Tipo contábil do lançamento (DEBIT ou CREDIT)")
        LedgerEntryType entryType,
        @Schema(description = "Tipo de operação financeira")
        LedgerOperationType operationType,
        @Schema(description = "Valor movimentado", example = "150.50")
        BigDecimal amount,
        @Schema(description = "Saldo resultante após o lançamento", example = "150.50")
        BigDecimal balanceAfter,
        @Schema(description = "Chave de idempotência do lançamento")
        String idempotencyKey,
        @Schema(description = "Descrição opcional do lançamento", example = "Depósito inicial")
        String description,
        @Schema(description = "Instante de criação do lançamento")
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionId(),
                entry.getAccountId(),
                entry.getEntryType(),
                entry.getOperationType(),
                entry.getAmount(),
                entry.getBalanceAfter(),
                entry.getIdempotencyKey(),
                entry.getDescription(),
                entry.getCreatedAt()
        );
    }
}
