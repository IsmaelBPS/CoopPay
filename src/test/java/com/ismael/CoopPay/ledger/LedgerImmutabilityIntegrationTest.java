package com.ismael.CoopPay.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.ismael.CoopPay.TestcontainersConfiguration;
import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerImmutabilityIntegrationTest {

    @Autowired
    private LedgerService ledgerService;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID entryId;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE TABLE ledger_entries");
        LedgerEntry entry = ledgerService.recordEntry(UUID.randomUUID(), com.ismael.CoopPay.TestData.createAccount(jdbc), LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT, new BigDecimal("10.00"), new BigDecimal("10.00"),
                "imm-" + UUID.randomUUID(), "Depósito");
        entryId = entry.getId();
    }

    @Test
    void shouldRejectUpdateAtDatabaseLevel() {
        assertThatThrownBy(() -> jdbc.update("UPDATE ledger_entries SET amount = 999 WHERE id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("imutável");
        assertThat(jdbc.queryForObject("SELECT amount FROM ledger_entries WHERE id = ?", BigDecimal.class, entryId))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void shouldRejectDeleteAtDatabaseLevel() {
        assertThatThrownBy(() -> jdbc.update("DELETE FROM ledger_entries WHERE id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("imutável");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ledger_entries", Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldRejectEntryForUnknownAccount() {
        assertThatThrownBy(() -> ledgerService.recordEntry(UUID.randomUUID(), UUID.randomUUID(), LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT, BigDecimal.ONE, BigDecimal.ONE, "orphan-" + UUID.randomUUID(), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
