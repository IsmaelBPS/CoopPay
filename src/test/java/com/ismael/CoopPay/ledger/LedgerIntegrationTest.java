package com.ismael.CoopPay.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ismael.CoopPay.TestcontainersConfiguration;
import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.ledger.domain.LedgerRepository;
import com.ismael.CoopPay.ledger.infrastructure.persistence.LedgerEntryJPARepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LedgerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LedgerService ledgerService;

    @MockitoSpyBean
    private LedgerRepository ledgerRepository;

    @Autowired
    private LedgerEntryJPARepository ledgerEntryJPARepository;

    @Value("${app.routes.api-base}")
    private String apiBase;

    @BeforeEach
    void setUp() {
        ledgerEntryJPARepository.deleteAll();
    }

    @Test
    void shouldRecordEntryAndQueryById() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        LedgerEntry recorded = ledgerService.recordEntry(
                transactionId,
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("250.00"),
                new BigDecimal("250.00"),
                "idem-deposit-" + UUID.randomUUID(),
                "Depósito em conta"
        );

        assertThat(recorded.getId()).isNotNull();
        assertThat(recorded.getCreatedAt()).isNotNull();

        mockMvc.perform(get(apiBase + "/ledger/{id}", recorded.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recorded.getId().toString()))
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()))
                .andExpect(jsonPath("$.entryType").value("CREDIT"))
                .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.balanceAfter").value(250.00))
                .andExpect(jsonPath("$.idempotencyKey").value(recorded.getIdempotencyKey()))
                .andExpect(jsonPath("$.description").value("Depósito em conta"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldReturnExistingEntryWithoutDuplicating_whenIdempotencyKeyRepeats() {
        UUID accountId = UUID.randomUUID();
        String idempotencyKey = "idem-repeat-" + UUID.randomUUID();

        LedgerEntry first = ledgerService.recordEntry(
                UUID.randomUUID(),
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                idempotencyKey,
                "Primeira chamada"
        );

        LedgerEntry second = ledgerService.recordEntry(
                UUID.randomUUID(),
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                idempotencyKey,
                "Segunda chamada"
        );

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(ledgerEntryJPARepository.count()).isEqualTo(1L);
    }

    @RepeatedTest(20)
    void shouldCreateSingleEntryAndReturnSameId_whenSameIdempotencyKeyIsRecordedConcurrently() throws Exception {
        UUID accountId = UUID.randomUUID();
        String idempotencyKey = "idem-race-" + UUID.randomUUID();
        int threads = 16;
        AtomicInteger lostRace = new AtomicInteger();
        doAnswer(inv -> {
            boolean inserted = (boolean) inv.callRealMethod();
            if (!inserted) {
                lostRace.incrementAndGet();
            }
            return inserted;
        }).when(ledgerRepository).insertIfAbsent(any());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<LedgerEntry>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return ledgerService.recordEntry(
                        UUID.randomUUID(), accountId,
                        LedgerEntryType.CREDIT, LedgerOperationType.DEPOSIT,
                        new BigDecimal("100.00"), new BigDecimal("100.00"),
                        idempotencyKey, "Corrida");
            }));
        }
        start.countDown();

        Set<UUID> ids = new HashSet<>();
        for (Future<LedgerEntry> f : futures) {
            ids.add(f.get(10, TimeUnit.SECONDS).getId());
        }
        pool.shutdown();

        assertThat(ids).hasSize(1);
        assertThat(ledgerEntryJPARepository.count()).isEqualTo(1L);
        // Prova que houve disputa real: todas menos uma perderam a corrida no ON CONFLICT.
        assertThat(lostRace.get()).isEqualTo(threads - 1);
    }

    @Test
    void shouldReturnAccountEntriesOrderedByCreatedAtDesc_andIsolateOtherAccounts() throws Exception {
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();
        Instant olderTime = Instant.parse("2026-11-23T10:00:00Z");
        Instant newerTime = Instant.parse("2026-11-23T12:00:00Z");

        LedgerEntry olderEntry = ledgerRepository.save(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountA,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                "idem-old-" + UUID.randomUUID(),
                "Depósito inicial",
                olderTime
        ));

        LedgerEntry newerEntry = ledgerRepository.save(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountA,
                LedgerEntryType.DEBIT,
                LedgerOperationType.WITHDRAWAL,
                new BigDecimal("120.00"),
                new BigDecimal("380.00"),
                "idem-new-" + UUID.randomUUID(),
                "Saque posterior",
                newerTime
        ));

        ledgerRepository.save(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountB,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("999.00"),
                new BigDecimal("999.00"),
                "idem-other-" + UUID.randomUUID(),
                "Outra conta",
                Instant.parse("2026-11-23T13:00:00Z")
        ));

        mockMvc.perform(get(apiBase + "/ledger/accounts/{accountId}", accountA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(newerEntry.getId().toString()))
                .andExpect(jsonPath("$[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$[1].id").value(olderEntry.getId().toString()))
                .andExpect(jsonPath("$[1].entryType").value("CREDIT"));
    }

    @Test
    void shouldReturnTransactionEntriesOrderedByCreatedAtAsc() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID otherTransactionId = UUID.randomUUID();
        UUID accountSource = UUID.randomUUID();
        UUID accountTarget = UUID.randomUUID();
        String idempotencyKey = "idem-tx-" + UUID.randomUUID();

        LedgerEntry debit = ledgerRepository.save(new LedgerEntry(
                null,
                transactionId,
                accountSource,
                LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("100.00"),
                new BigDecimal("400.00"),
                idempotencyKey,
                "Transferência enviada",
                Instant.parse("2026-11-23T11:00:00Z")
        ));

        LedgerEntry credit = ledgerRepository.save(new LedgerEntry(
                null,
                transactionId,
                accountTarget,
                LedgerEntryType.CREDIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                idempotencyKey,
                "Transferência recebida",
                Instant.parse("2026-11-23T11:00:01Z")
        ));

        ledgerRepository.save(new LedgerEntry(
                null,
                otherTransactionId,
                accountSource,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("50.00"),
                new BigDecimal("450.00"),
                "idem-other-tx-" + UUID.randomUUID(),
                "Outra transação",
                Instant.parse("2026-11-23T11:05:00Z")
        ));

        mockMvc.perform(get(apiBase + "/ledger/transactions/{transactionId}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(debit.getId().toString()))
                .andExpect(jsonPath("$[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$[1].id").value(credit.getId().toString()))
                .andExpect(jsonPath("$[1].entryType").value("CREDIT"));
    }

    @Test
    void shouldReturn404ProblemDetailWhenFindingNonExistentLedgerEntry() throws Exception {
        UUID randomId = UUID.randomUUID();
        String path = apiBase + "/ledger/" + randomId;

        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Lançamento não encontrado: " + randomId))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
