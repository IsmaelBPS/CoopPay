package com.ismael.CoopPay.account.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ismael.CoopPay.account.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@Tag(name = "Contas", description = "Abertura de contas, saldo, depósito, saque e transferência")
@RestController
@RequestMapping("${app.routes.api-base}/accounts")
@RequiredArgsConstructor
public class AccountController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final AccountService service;

    @Operation(summary = "Abre conta para um associado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta aberta"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Associado não encontrado"),
        @ApiResponse(responseCode = "409", description = "Associado já possui conta")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody OpenAccountRequest request) {
        return AccountResponse.from(service.open(request.associateId()));
    }

    @Operation(summary = "Busca conta por ID")
    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable UUID id) {
        return AccountResponse.from(service.findById(id));
    }

    @Operation(summary = "Consulta saldo da conta")
    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable UUID id) {
        return BalanceResponse.from(service.findById(id));
    }

    @Operation(summary = "Deposita valor na conta",
            description = "Header opcional Idempotency-Key: repetir a chave não duplica o depósito.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Depósito realizado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta inativa ou chave de idempotência em conflito")
    })
    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(
            @PathVariable UUID id,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.deposit(id, request.amount(), idempotencyKey));
    }

    @Operation(summary = "Saca valor da conta")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saque realizado"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta inativa ou chave de idempotência em conflito"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(
            @PathVariable UUID id,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(service.withdraw(id, request.amount(), idempotencyKey));
    }

    @Operation(summary = "Transfere valor da conta para outra conta", description = "Retorna a conta de origem atualizada.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transferência realizada"),
        @ApiResponse(responseCode = "400", description = "Origem e destino iguais ou dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
        @ApiResponse(responseCode = "409", description = "Conta inativa ou chave de idempotência em conflito"),
        @ApiResponse(responseCode = "422", description = "Saldo insuficiente")
    })
    @PostMapping("/{id}/transfers")
    public AccountResponse transfer(
            @PathVariable UUID id,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return AccountResponse.from(service.transfer(id, request.targetAccountId(), request.amount(), idempotencyKey));
    }
}
