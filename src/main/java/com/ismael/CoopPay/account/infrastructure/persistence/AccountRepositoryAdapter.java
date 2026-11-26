package com.ismael.CoopPay.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ismael.CoopPay.account.domain.Account;
import com.ismael.CoopPay.account.domain.AccountRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJPARepository jpaRepository;

    @Override
    public Account save(Account account) {
        return jpaRepository.save(AccountEntity.fromDomain(account)).toDomain();
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaRepository.findById(id).map(AccountEntity::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(AccountEntity::toDomain);
    }

    @Override
    public boolean existsByAssociateId(UUID associateId) {
        return jpaRepository.existsByAssociateId(associateId);
    }
}
