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
import org.springframework.transaction.support.TransactionTemplate;

import com.ismael.CoopPay.TestcontainersConfiguration;
import com.ismael.CoopPay.ledger.domain.LedgerEntry;
import com.ismael.CoopPay.ledger.domain.LedgerEntryType;
import com.ismael.CoopPay.ledger.domain.LedgerOperationType;
import com.ismael.CoopPay.ledger.infrastructure.persistence.LedgerEntryEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LedgerEntryEntityIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status ->
            entityManager.createNativeQuery("DELETE FROM ledger_entries").executeUpdate()
        );
    }

    @Test
    void shouldPersistAndConvertLedgerEntryFromAndToDomain() {
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        LedgerEntry domainEntry = new LedgerEntry(
            null,
            transactionId,
            accountId,
            LedgerEntryType.CREDIT,
            LedgerOperationType.DEPOSIT,
            new BigDecimal("150.50"),
            new BigDecimal("150.50"),
            "idem-" + UUID.randomUUID(),
            "Depósito inicial",
            null
        );

        UUID persistedId = transactionTemplate.execute(status -> {
            LedgerEntryEntity entity = LedgerEntryEntity.fromDomain(domainEntry);
            entityManager.persist(entity);
            entityManager.flush();
            return entity.getId();
        });

        assertThat(persistedId).isNotNull();

        LedgerEntryEntity reloaded = transactionTemplate.execute(status ->
            entityManager.find(LedgerEntryEntity.class, persistedId)
        );

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getAmount()).isEqualByComparingTo("150.50");
        assertThat(reloaded.getBalanceAfter()).isEqualByComparingTo("150.50");

        LedgerEntry roundTrip = reloaded.toDomain();
        assertThat(roundTrip.getId()).isEqualTo(persistedId);
        assertThat(roundTrip.getTransactionId()).isEqualTo(transactionId);
        assertThat(roundTrip.getAccountId()).isEqualTo(accountId);
        assertThat(roundTrip.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(roundTrip.getOperationType()).isEqualTo(LedgerOperationType.DEPOSIT);
        assertThat(roundTrip.getAmount()).isEqualByComparingTo("150.50");
        assertThat(roundTrip.getBalanceAfter()).isEqualByComparingTo("150.50");
        assertThat(roundTrip.getIdempotencyKey()).isEqualTo(domainEntry.getIdempotencyKey());
        assertThat(roundTrip.getDescription()).isEqualTo("Depósito inicial");
        assertThat(roundTrip.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldAllowDebitAndCreditForSameIdempotencyKeyAcrossDifferentAccounts() {
        UUID transactionId = UUID.randomUUID();
        UUID accountA = UUID.randomUUID();
        UUID accountB = UUID.randomUUID();
        String idempotencyKey = "idem-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            LedgerEntryEntity debit = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                transactionId,
                accountA,
                LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("100.00"),
                new BigDecimal("400.00"),
                idempotencyKey,
                "Transferência enviada",
                null
            ));
            LedgerEntryEntity credit = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                transactionId,
                accountB,
                LedgerEntryType.CREDIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("100.00"),
                new BigDecimal("200.00"),
                idempotencyKey,
                "Transferência recebida",
                null
            ));
            entityManager.persist(debit);
            entityManager.persist(credit);
            entityManager.flush();
        });

        Long count = transactionTemplate.execute(status ->
            entityManager.createQuery("SELECT COUNT(l) FROM LedgerEntryEntity l", Long.class).getSingleResult()
        );
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void shouldRejectDuplicateIdempotencyKeyForSameAccountAndEntryType() {
        UUID accountA = UUID.randomUUID();
        String idempotencyKey = "idem-" + UUID.randomUUID();

        transactionTemplate.executeWithoutResult(status -> {
            LedgerEntryEntity first = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountA,
                LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                idempotencyKey,
                "Primeira tentativa",
                null
            ));
            entityManager.persist(first);
            entityManager.flush();
        });

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            LedgerEntryEntity duplicate = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountA,
                LedgerEntryType.DEBIT,
                LedgerOperationType.TRANSFER,
                new BigDecimal("50.00"),
                new BigDecimal("50.00"),
                idempotencyKey,
                "Tentativa duplicada",
                null
            ));
            entityManager.persist(duplicate);
            entityManager.flush();
        })).isInstanceOf(Exception.class);
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            LedgerEntryEntity zeroEntry = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                "idem-" + UUID.randomUUID(),
                "Valor zero",
                null
            ));
            entityManager.persist(zeroEntry);
            entityManager.flush();
        })).isInstanceOf(Exception.class);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            LedgerEntryEntity negativeEntry = LedgerEntryEntity.fromDomain(new LedgerEntry(
                null,
                UUID.randomUUID(),
                accountId,
                LedgerEntryType.CREDIT,
                LedgerOperationType.DEPOSIT,
                new BigDecimal("-10.00"),
                new BigDecimal("100.00"),
                "idem-" + UUID.randomUUID(),
                "Valor negativo",
                null
            ));
            entityManager.persist(negativeEntry);
            entityManager.flush();
        })).isInstanceOf(Exception.class);
    }
}
