package com.ismael.CoopPay.ledger.api;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ismael.CoopPay.ledger.LedgerService;
import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LedgerService service;

    @Value("${app.routes.api-base}")
    private String apiBase;

    @Test
    void getById_shouldReturn200AndLedgerEntryResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-23T14:30:00Z");
        LedgerEntry entry = new LedgerEntry(
                id,
                transactionId,
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("150.50"),
                new BigDecimal("350.50"),
                "idem-1",
                "Depósito inicial",
                createdAt
        );
        when(service.findById(id)).thenReturn(entry);

        mockMvc.perform(get(apiBase + "/ledger/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.entryType").value("CREDIT"))
                .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(150.50))
                .andExpect(jsonPath("$.balanceAfter").value(350.50))
                .andExpect(jsonPath("$.idempotencyKey").value("idem-1"))
                .andExpect(jsonPath("$.description").value("Depósito inicial"))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));
    }

    @Test
    void getById_shouldReturn400_whenIdIsNotValidUuid() throws Exception {
        mockMvc.perform(get(apiBase + "/ledger/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getByAccountId_shouldReturn200AndListOfEntries() throws Exception {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-23T14:35:00Z");
        LedgerEntry entry = new LedgerEntry(
                id,
                transactionId,
                accountId,
                LedgerEntryType.DEBIT,
                LedgerOperationType.WITHDRAWAL,
                new BigDecimal("40.00"),
                new BigDecimal("60.00"),
                "idem-acc-1",
                "Saque",
                createdAt
        );
        when(service.findByAccountId(accountId)).thenReturn(List.of(entry));

        mockMvc.perform(get(apiBase + "/ledger/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$[0].operationType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$[0].amount").value(40.00))
                .andExpect(jsonPath("$[0].balanceAfter").value(60.00))
                .andExpect(jsonPath("$[0].idempotencyKey").value("idem-acc-1"))
                .andExpect(jsonPath("$[0].description").value("Saque"))
                .andExpect(jsonPath("$[0].createdAt").value(createdAt.toString()));
    }

    @Test
    void getByAccountId_shouldReturn200AndEmptyList_whenNoEntriesExist() throws Exception {
        UUID accountId = UUID.randomUUID();
        when(service.findByAccountId(accountId)).thenReturn(List.of());

        mockMvc.perform(get(apiBase + "/ledger/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByAccountId_shouldReturn400_whenAccountIdIsNotValidUuid() throws Exception {
        mockMvc.perform(get(apiBase + "/ledger/accounts/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void getByTransactionId_shouldReturn200AndListOfEntries() throws Exception {
        UUID id = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-23T14:40:00Z");
        LedgerEntry entry = new LedgerEntry(
                id,
                transactionId,
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("200.00"),
                new BigDecimal("500.00"),
                "idem-tx-1",
                "Transferência recebida",
                createdAt
        );
        when(service.findByTransactionId(transactionId)).thenReturn(List.of(entry));

        mockMvc.perform(get(apiBase + "/ledger/transactions/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$[0].entryType").value("CREDIT"))
                .andExpect(jsonPath("$[0].operationType").value("TRANSFER"))
                .andExpect(jsonPath("$[0].amount").value(200.00))
                .andExpect(jsonPath("$[0].balanceAfter").value(500.00))
                .andExpect(jsonPath("$[0].idempotencyKey").value("idem-tx-1"))
                .andExpect(jsonPath("$[0].description").value("Transferência recebida"))
                .andExpect(jsonPath("$[0].createdAt").value(createdAt.toString()));
    }

    @Test
    void getByTransactionId_shouldReturn200AndEmptyList_whenNoEntriesExist() throws Exception {
        UUID transactionId = UUID.randomUUID();
        when(service.findByTransactionId(transactionId)).thenReturn(List.of());

        mockMvc.perform(get(apiBase + "/ledger/transactions/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getByTransactionId_shouldReturn400_whenTransactionIdIsNotValidUuid() throws Exception {
        mockMvc.perform(get(apiBase + "/ledger/transactions/not-a-uuid"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
