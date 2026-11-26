package com.ismael.CoopPay.account.domain;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {
    Account save(Account account);

    Optional<Account> findById(UUID id);

    /** Carrega a conta com lock pessimista (SELECT ... FOR UPDATE); exige transação ativa. */
    Optional<Account> findByIdForUpdate(UUID id);

    boolean existsByAssociateId(UUID associateId);
}
