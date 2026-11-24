package com.ismael.CoopPay.associate;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ismael.CoopPay.associate.domain.Associate;
import com.ismael.CoopPay.associate.domain.AssociateRepository;
import com.ismael.CoopPay.associate.domain.AssociateType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssociateService {

    private final AssociateRepository associateRepository;

    @Transactional
    public Associate create(String name, String email, String password, AssociateType type) {
        if (associateRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado: " + email);
        }
        Associate associate = new Associate(null, name, email, password, type);
        Associate saved = associateRepository.save(associate);
        log.info("Associado criado: associateId={}, type={}", saved.getId(), saved.getType());
        return saved;
    }

    @Transactional(readOnly = true)
    public Associate findById(UUID id) {
        return associateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Associado não encontrado: " + id));
    }

    @Transactional
    public Associate update(UUID id, String name, String email, AssociateType type) {
        Associate associate = findById(id);
        if (!associate.getEmail().equals(email) && associateRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado: " + email);
        }
        associate.setName(name);
        associate.setEmail(email);
        associate.setType(type);
        Associate updated = associateRepository.update(associate);
        log.info("Associado atualizado: associateId={}, type={}", updated.getId(), updated.getType());
        return updated;
    }

    @Transactional
    public void delete(UUID id) {
        Associate associate = findById(id);
        associateRepository.delete(associate);
        log.info("Associado removido: associateId={}", id);
    }
}
