package com.ismael.CoopPay.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.ismael.CoopPay.TestcontainersConfiguration;
import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.infrastructure.persistence.AccountJPARepository;
import com.ismael.CoopPay.associate.AssociateService;
import com.ismael.CoopPay.associate.domain.AssociateType;
import com.ismael.CoopPay.associate.infrastructure.persistence.AssociateJPARepository;
import com.ismael.CoopPay.ledger.infrastructure.persistence.LedgerEntryJPARepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountIntegrationTest {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountService accountService;
    @Autowired
    private AssociateService associateService;
    @Autowired
    private AccountJPARepository accountJpa;
    @Autowired
    private AssociateJPARepository associateJpa;
    @Autowired
    private LedgerEntryJPARepository ledgerJpa;

    @Value("${app.routes.api-base}")
    private String apiBase;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE ledger_entries, accounts");
        accountJpa.deleteAll();
        associateJpa.deleteAll();
    }

    private Account newAccount() {
        var associate = associateService.create("Assoc", UUID.randomUUID() + "@cooppay.com", "secret1", AssociateType.INDIVIDUAL);
        return accountService.open(associate.getId());
    }

    private String body(String amount) {
        return "{\"amount\": " + amount + "}";
    }

    @Test
    void shouldOpenDepositWithdrawAndShowBalanceAndLedger() throws Exception {
        var associate = associateService.create("Maria", "maria@cooppay.com", "secret1", AssociateType.INDIVIDUAL);

        String response = mockMvc.perform(post(apiBase + "/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\": \"" + associate.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.balance").value(0.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post(apiBase + "/accounts/{id}/deposit", id).header(IDEMPOTENCY_HEADER, "dep-" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("100.50")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(100.50));
        mockMvc.perform(post(apiBase + "/accounts/{id}/withdraw", id).header(IDEMPOTENCY_HEADER, "wd-" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("40.25")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(60.25));
        mockMvc.perform(post(apiBase + "/accounts/{id}/withdraw", id).header(IDEMPOTENCY_HEADER, "wd-big-" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(body("1000")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get(apiBase + "/accounts/{id}/balance", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(60.25));
        mockMvc.perform(get(apiBase + "/ledger/accounts/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRejectInvalidInputAndUnknownAccount() throws Exception {
        Account account = newAccount();
        mockMvc.perform(post(apiBase + "/accounts/{id}/deposit", account.getId()).header(IDEMPOTENCY_HEADER, "k-invalid")
                        .contentType(MediaType.APPLICATION_JSON).content(body("0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(apiBase + "/accounts/{id}/deposit", UUID.randomUUID()).header(IDEMPOTENCY_HEADER, "k-unknown")
                        .contentType(MediaType.APPLICATION_JSON).content(body("1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(apiBase + "/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMovementsWithoutIdempotencyKeyHeader() throws Exception {
        Account a = newAccount();
        Account b = newAccount();
        accountService.deposit(a.getId(), new BigDecimal("100.00"), "seed");

        mockMvc.perform(post(apiBase + "/accounts/{id}/deposit", a.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body("10.00")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(apiBase + "/accounts/{id}/withdraw", a.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body("10.00")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(apiBase + "/accounts/{id}/transfers", a.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\": \"" + b.getId() + "\", \"amount\": 10.00}"))
                .andExpect(status().isBadRequest());

        assertThat(accountService.findById(a.getId()).getBalance()).isEqualByComparingTo("100.00");
        assertThat(ledgerJpa.count()).isEqualTo(1L);
    }

    @Test
    void shouldRejectBlankIdempotencyKeyHeader() throws Exception {
        Account account = newAccount();
        mockMvc.perform(post(apiBase + "/accounts/{id}/deposit", account.getId()).header(IDEMPOTENCY_HEADER, "   ")
                        .contentType(MediaType.APPLICATION_JSON).content(body("10.00")))
                .andExpect(status().isBadRequest());
        assertThat(ledgerJpa.count()).isZero();
    }

    @Test
    void shouldRejectSecondAccountForSameAssociate() throws Exception {
        Account account = newAccount();
        mockMvc.perform(post(apiBase + "/accounts").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associateId\": \"" + account.getAssociateId() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldNotDepositTwice_whenIdempotencyKeyRepeats() {
        Account account = newAccount();
        accountService.deposit(account.getId(), new BigDecimal("10.00"), "dep-1");
        accountService.deposit(account.getId(), new BigDecimal("10.00"), "dep-1");

        assertThat(accountService.findById(account.getId()).getBalance()).isEqualByComparingTo("10.00");
        assertThat(ledgerJpa.count()).isEqualTo(1L);
    }

    @Test
    void shouldTransferBetweenAccountsWithTwoLedgerEntries() throws Exception {
        Account a = newAccount();
        Account b = newAccount();
        accountService.deposit(a.getId(), new BigDecimal("100.00"), "seed");

        mockMvc.perform(post(apiBase + "/accounts/{id}/transfers", a.getId()).header(IDEMPOTENCY_HEADER, "tr-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\": \"" + b.getId() + "\", \"amount\": 30.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(70.00));

        assertThat(accountService.findById(b.getId()).getBalance()).isEqualByComparingTo("30.00");
        assertThat(ledgerJpa.count()).isEqualTo(3L);

        mockMvc.perform(post(apiBase + "/accounts/{id}/transfers", a.getId()).header(IDEMPOTENCY_HEADER, "tr-self")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetAccountId\": \"" + a.getId() + "\", \"amount\": 1.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentWithdrawals_shouldNeverGoNegative() throws Exception {
        Account account = newAccount();
        accountService.deposit(account.getId(), new BigDecimal("100.00"), "seed");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    accountService.withdraw(account.getId(), new BigDecimal("10.00"), UUID.randomUUID().toString());
                    return true;
                } catch (RuntimeException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int ok = 0;
        for (Future<Boolean> f : results) {
            if (f.get(30, TimeUnit.SECONDS)) ok++;
        }
        pool.shutdownNow();

        assertThat(ok).isEqualTo(10);
        assertThat(accountService.findById(account.getId()).getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void crossedConcurrentTransfers_shouldNotDeadlockAndPreserveTotal() throws Exception {
        Account a = newAccount();
        Account b = newAccount();
        accountService.deposit(a.getId(), new BigDecimal("500.00"), "seed-a");
        accountService.deposit(b.getId(), new BigDecimal("500.00"), "seed-b");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            boolean forward = i % 2 == 0;
            futures.add(pool.submit(() -> {
                start.await();
                if (forward) accountService.transfer(a.getId(), b.getId(), new BigDecimal("5.00"), UUID.randomUUID().toString());
                else accountService.transfer(b.getId(), a.getId(), new BigDecimal("5.00"), UUID.randomUUID().toString());
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        BigDecimal total = accountService.findById(a.getId()).getBalance().add(accountService.findById(b.getId()).getBalance());
        assertThat(total).isEqualByComparingTo("1000.00");
    }
}
