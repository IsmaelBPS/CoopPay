package com.ismael.CoopPay.associate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.ismael.CoopPay.TestcontainersConfiguration;
import com.ismael.CoopPay.associate.api.AssociateResponse;
import com.ismael.CoopPay.associate.api.CreateAssociateRequest;
import com.ismael.CoopPay.associate.api.UpdateAssociateRequest;
import com.ismael.CoopPay.associate.domain.AssociateType;
import com.ismael.CoopPay.associate.infrastructure.persistence.AssociateEntity;
import com.ismael.CoopPay.associate.infrastructure.persistence.AssociateJPARepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AssociateIntegrationTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssociateJPARepository associateJPARepository;

    @Value("${app.routes.api-base}")
    private String apiBase;

    @BeforeEach
    void setUp() {
        associateJPARepository.deleteAll();
    }

    private AssociateResponse createAssociateViaApi(String name, String email, String password, AssociateType type) throws Exception {
        CreateAssociateRequest request = new CreateAssociateRequest(name, email, password, type);
        MvcResult result = mockMvc.perform(post(apiBase + "/associates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AssociateResponse.class);
    }

    @Test
    void shouldCreateAndPersistAssociate() throws Exception {
        AssociateResponse created = createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);

        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Alice");
        assertThat(created.email()).isEqualTo("alice@coop.com");
        assertThat(created.type()).isEqualTo(AssociateType.INDIVIDUAL);

        AssociateEntity persisted = associateJPARepository.findById(created.id()).orElseThrow();
        assertThat(persisted.getName()).isEqualTo("Alice");
        assertThat(persisted.getEmail()).isEqualTo("alice@coop.com");
        assertThat(persisted.getPassword()).startsWith("$argon2id$").isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", persisted.getPassword())).isTrue();
        assertThat(persisted.getType()).isEqualTo(AssociateType.INDIVIDUAL);
    }

    @Test
    void shouldFindAssociateById() throws Exception {
        AssociateResponse created = createAssociateViaApi("Bob", "bob@coop.com", "pass456", AssociateType.COMPANY);

        mockMvc.perform(get(apiBase + "/associates/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.email").value("bob@coop.com"))
                .andExpect(jsonPath("$.type").value("COMPANY"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldUpdateAssociateAndPreservePassword() throws Exception {
        AssociateResponse created = createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        String originalHash = associateJPARepository.findById(created.id()).orElseThrow().getPassword();
        UpdateAssociateRequest updateRequest = new UpdateAssociateRequest("Alice Corp", "corp@coop.com", AssociateType.COMPANY);

        mockMvc.perform(put(apiBase + "/associates/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id().toString()))
                .andExpect(jsonPath("$.name").value("Alice Corp"))
                .andExpect(jsonPath("$.email").value("corp@coop.com"))
                .andExpect(jsonPath("$.type").value("COMPANY"));

        AssociateEntity updated = associateJPARepository.findById(created.id()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Alice Corp");
        assertThat(updated.getEmail()).isEqualTo("corp@coop.com");
        assertThat(updated.getType()).isEqualTo(AssociateType.COMPANY);
        assertThat(updated.getPassword()).isEqualTo(originalHash);
    }

    @Test
    void shouldUpdateAssociateKeepingSameEmail() throws Exception {
        AssociateResponse created = createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        UpdateAssociateRequest updateRequest = new UpdateAssociateRequest("Alice Updated", "alice@coop.com", AssociateType.INDIVIDUAL);

        mockMvc.perform(put(apiBase + "/associates/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.email").value("alice@coop.com"));
    }

    @Test
    void shouldDeleteAssociate() throws Exception {
        AssociateResponse created = createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);

        mockMvc.perform(delete(apiBase + "/associates/{id}", created.id()))
                .andExpect(status().isNoContent());

        assertThat(associateJPARepository.existsById(created.id())).isFalse();
    }

    @Test
    void shouldReturn409ProblemDetailWhenCreatingAssociateWithDuplicateEmail() throws Exception {
        createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        CreateAssociateRequest duplicate = new CreateAssociateRequest("Alice 2", "alice@coop.com", "other", AssociateType.COMPANY);

        mockMvc.perform(post(apiBase + "/associates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de recurso"))
                .andExpect(jsonPath("$.detail").value("Email já cadastrado: alice@coop.com"))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        assertThat(associateJPARepository.count()).isEqualTo(1);
    }

    @Test
    void shouldReturn404ProblemDetailWhenFindingNonExistentAssociate() throws Exception {
        UUID randomId = UUID.randomUUID();
        String path = apiBase + "/associates/" + randomId;

        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Associado não encontrado: " + randomId))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn409ProblemDetailWhenUpdatingToExistingEmail() throws Exception {
        AssociateResponse associate1 = createAssociateViaApi("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        AssociateResponse associate2 = createAssociateViaApi("Bob", "bob@coop.com", "pass456", AssociateType.INDIVIDUAL);

        UpdateAssociateRequest update = new UpdateAssociateRequest("Bob", associate1.email(), AssociateType.INDIVIDUAL);
        String path = apiBase + "/associates/" + associate2.id();

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de recurso"))
                .andExpect(jsonPath("$.detail").value("Email já cadastrado: " + associate1.email()))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn404ProblemDetailWhenDeletingNonExistentAssociate() throws Exception {
        UUID randomId = UUID.randomUUID();
        String path = apiBase + "/associates/" + randomId;

        mockMvc.perform(delete(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Associado não encontrado: " + randomId))
                .andExpect(jsonPath("$.instance").value(path))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
