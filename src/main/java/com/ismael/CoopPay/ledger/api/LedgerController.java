package com.ismael.CoopPay.ledger.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ismael.CoopPay.ledger.LedgerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Ledger", description = "Consulta de lançamentos e extrato imutável")
@RestController
@RequestMapping("${app.routes.api-base}/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService service;

    @Operation(summary = "Busca lançamento por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lançamento encontrado"),
        @ApiResponse(responseCode = "400", description = "UUID inválido"),
        @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
    })
    @GetMapping("/{id}")
    public LedgerEntryResponse getById(@PathVariable UUID id) {
        return LedgerEntryResponse.from(service.findById(id));
    }

    @Operation(summary = "Lista lançamentos de uma conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Extrato retornado"),
        @ApiResponse(responseCode = "400", description = "UUID inválido")
    })
    @GetMapping("/accounts/{accountId}")
    public List<LedgerEntryResponse> getByAccountId(@PathVariable UUID accountId) {
        return service.findByAccountId(accountId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }

    @Operation(summary = "Lista lançamentos de uma transação")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lançamentos retornados"),
        @ApiResponse(responseCode = "400", description = "UUID inválido")
    })
    @GetMapping("/transactions/{transactionId}")
    public List<LedgerEntryResponse> getByTransactionId(@PathVariable UUID transactionId) {
        return service.findByTransactionId(transactionId)
                .stream()
                .map(LedgerEntryResponse::from)
                .toList();
    }
}
