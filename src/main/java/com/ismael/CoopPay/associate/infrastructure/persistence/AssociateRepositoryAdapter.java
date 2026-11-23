package com.ismael.CoopPay.associate.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.ismael.CoopPay.associate.domain.Associate;
import com.ismael.CoopPay.associate.domain.AssociateRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AssociateRepositoryAdapter implements AssociateRepository {

    private final AssociateJPARepository jpaRepository;

    @Override
    public Associate save(Associate associate) {
        AssociateEntity saved = jpaRepository.save(AssociateEntity.fromDomain(associate));
        return saved.toDomain();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<Associate> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(AssociateEntity::toDomain);
    }

    @Override
    public Optional<Associate> findById(UUID id) {
        return jpaRepository.findById(id).map(AssociateEntity::toDomain);
    }

    @Override
    public boolean delete(Associate associate) {
        if (associate.getId() == null || !jpaRepository.existsById(associate.getId())) {
            return false;
        }

        jpaRepository.deleteById(associate.getId());
        return true;
    }

    @Override
    public Associate update(Associate associate) {
        return save(associate);
    }
}
