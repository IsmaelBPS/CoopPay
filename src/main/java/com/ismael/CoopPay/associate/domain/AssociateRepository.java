package com.ismael.CoopPay.associate.domain;

import java.util.Optional;
import java.util.UUID;

public interface AssociateRepository {

    Associate save(Associate associate);

    boolean delete(Associate associate);

    Associate update(Associate associate);

    boolean existsByEmail(String email);

    Optional<Associate> findByEmail(String email);

    Optional<Associate> findById(UUID id);
}
