package com.ismael.CoopPay.associate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ismael.CoopPay.associate.domain.Associate;
import com.ismael.CoopPay.associate.domain.AssociateRepository;
import com.ismael.CoopPay.associate.domain.AssociateType;
import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class AssociateServiceTest {

    @Mock
    private AssociateRepository associateRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AssociateService associateService;

    @Test
    void create_shouldSaveAndReturnAssociate_whenEmailDoesNotExist() {
        UUID generatedId = UUID.randomUUID();
        when(associateRepository.existsByEmail("alice@coop.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(associateRepository.save(any(Associate.class))).thenAnswer(inv -> {
            Associate arg = inv.getArgument(0);
            return new Associate(generatedId, arg.getName(), arg.getEmail(), arg.getPassword(), arg.getType());
        });

        Associate result = associateService.create("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);

        assertThat(result.getId()).isEqualTo(generatedId);
        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getEmail()).isEqualTo("alice@coop.com");
        assertThat(result.getPassword()).isEqualTo("hashed");
        assertThat(result.getType()).isEqualTo(AssociateType.INDIVIDUAL);

        ArgumentCaptor<Associate> captor = ArgumentCaptor.forClass(Associate.class);
        verify(associateRepository).save(captor.capture());
        Associate captured = captor.getValue();
        assertThat(captured.getId()).isNull();
        assertThat(captured.getName()).isEqualTo("Alice");
        assertThat(captured.getEmail()).isEqualTo("alice@coop.com");
        assertThat(captured.getPassword()).isEqualTo("hashed");
        assertThat(captured.getType()).isEqualTo(AssociateType.INDIVIDUAL);
        assertThat(meterRegistry.counter("cooppay.associates.operations", "action", "created").count()).isEqualTo(1.0);
    }

    @Test
    void create_shouldThrow_whenEmailAlreadyExists() {
        String email = "alice@coop.com";
        when(associateRepository.existsByEmail(email)).thenReturn(true);

        assertThatThrownBy(() -> associateService.create("Alice", email, "secret123", AssociateType.INDIVIDUAL))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email já cadastrado: " + email);

        verify(associateRepository, never()).save(any());
    }

    @Test
    void findById_shouldReturnAssociate_whenFound() {
        UUID id = UUID.randomUUID();
        Associate expected = new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(associateRepository.findById(id)).thenReturn(Optional.of(expected));

        Associate result = associateService.findById(id);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(associateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> associateService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Associado não encontrado: " + id);
    }

    @Test
    void update_shouldUpdateAssociate_whenEmailUnchanged() {
        UUID id = UUID.randomUUID();
        Associate existing = new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(associateRepository.findById(id)).thenReturn(Optional.of(existing));
        when(associateRepository.update(any(Associate.class))).thenAnswer(inv -> inv.getArgument(0));

        Associate updated = associateService.update(id, "Alice Silva", "alice@coop.com", AssociateType.COMPANY);

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getName()).isEqualTo("Alice Silva");
        assertThat(updated.getEmail()).isEqualTo("alice@coop.com");
        assertThat(updated.getPassword()).isEqualTo("secret123");
        assertThat(updated.getType()).isEqualTo(AssociateType.COMPANY);

        verify(associateRepository, never()).existsByEmail(anyString());
        verify(associateRepository).update(existing);
        assertThat(meterRegistry.counter("cooppay.associates.operations", "action", "updated").count()).isEqualTo(1.0);
    }

    @Test
    void update_shouldUpdateAssociate_whenNewEmailIsAvailable() {
        UUID id = UUID.randomUUID();
        Associate existing = new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(associateRepository.findById(id)).thenReturn(Optional.of(existing));
        when(associateRepository.existsByEmail("alice.new@coop.com")).thenReturn(false);
        when(associateRepository.update(any(Associate.class))).thenAnswer(inv -> inv.getArgument(0));

        Associate updated = associateService.update(id, "Alice Silva", "alice.new@coop.com", AssociateType.COMPANY);

        assertThat(updated.getName()).isEqualTo("Alice Silva");
        assertThat(updated.getEmail()).isEqualTo("alice.new@coop.com");
        assertThat(updated.getType()).isEqualTo(AssociateType.COMPANY);
        assertThat(updated.getPassword()).isEqualTo("secret123");
        verify(associateRepository).update(existing);
    }

    @Test
    void update_shouldThrow_whenAssociateNotFound() {
        UUID id = UUID.randomUUID();
        when(associateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> associateService.update(id, "Alice", "alice@coop.com", AssociateType.INDIVIDUAL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Associado não encontrado");

        verify(associateRepository, never()).update(any());
    }

    @Test
    void update_shouldThrow_whenNewEmailAlreadyExists() {
        UUID id = UUID.randomUUID();
        Associate existing = new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(associateRepository.findById(id)).thenReturn(Optional.of(existing));
        when(associateRepository.existsByEmail("taken@coop.com")).thenReturn(true);

        assertThatThrownBy(() -> associateService.update(id, "Alice", "taken@coop.com", AssociateType.INDIVIDUAL))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Email já cadastrado");

        verify(associateRepository, never()).update(any());
    }

    @Test
    void delete_shouldDeleteAssociate_whenFound() {
        UUID id = UUID.randomUUID();
        Associate existing = new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(associateRepository.findById(id)).thenReturn(Optional.of(existing));

        associateService.delete(id);

        verify(associateRepository).delete(existing);
        assertThat(meterRegistry.counter("cooppay.associates.operations", "action", "deleted").count()).isEqualTo(1.0);
    }

    @Test
    void delete_shouldThrow_whenAssociateNotFound() {
        UUID id = UUID.randomUUID();
        when(associateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> associateService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Associado não encontrado");

        verify(associateRepository, never()).delete(any());
    }
}
