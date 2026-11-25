package com.ismael.CoopPay.associate.api;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ismael.CoopPay.associate.AssociateService;
import com.ismael.CoopPay.associate.domain.Associate;
import com.ismael.CoopPay.associate.domain.AssociateType;
import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AssociateController.class)
class AssociateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssociateService service;

    @Value("${app.routes.api-base}")
    private String apiBase;

    @Test
    void create_shouldReturn201AndAssociateResponse_withoutPassword() throws Exception {
        UUID id = UUID.randomUUID();
        CreateAssociateRequest request = new CreateAssociateRequest("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(service.create("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL))
                .thenReturn(new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL));

        mockMvc.perform(post(apiBase + "/associates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@coop.com"))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void create_shouldReturn400_whenPayloadIsInvalid() throws Exception {
        CreateAssociateRequest invalid = new CreateAssociateRequest("", "not-an-email", "", null);

        mockMvc.perform(post(apiBase + "/associates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos estão inválidos"))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "name", "password", "type")));

        verifyNoInteractions(service);
    }

    @Test
    void create_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        CreateAssociateRequest request = new CreateAssociateRequest("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL);
        when(service.create("Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL))
                .thenThrow(new ResourceConflictException("Email já cadastrado: alice@coop.com"));

        mockMvc.perform(post(apiBase + "/associates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de recurso"))
                .andExpect(jsonPath("$.detail").value("Email já cadastrado: alice@coop.com"))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getAssociate_shouldReturn200AndAssociateResponse() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id))
                .thenReturn(new Associate(id, "Alice", "alice@coop.com", "secret123", AssociateType.INDIVIDUAL));

        mockMvc.perform(get(apiBase + "/associates/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@coop.com"))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void getAssociate_shouldReturn400_whenIdIsNotValidUuid() throws Exception {
        mockMvc.perform(get(apiBase + "/associates/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Parâmetro 'id' inválido: 'not-a-uuid'"))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates/not-a-uuid"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void getAssociate_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResourceNotFoundException("Associado não encontrado: " + id));

        mockMvc.perform(get(apiBase + "/associates/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Associado não encontrado: " + id))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates/" + id))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void update_shouldReturn200AndUpdatedAssociateResponse() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateAssociateRequest request = new UpdateAssociateRequest("Alice Silva", "alice.new@coop.com", AssociateType.COMPANY);
        when(service.update(id, "Alice Silva", "alice.new@coop.com", AssociateType.COMPANY))
                .thenReturn(new Associate(id, "Alice Silva", "alice.new@coop.com", "secret123", AssociateType.COMPANY));

        mockMvc.perform(put(apiBase + "/associates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Alice Silva"))
                .andExpect(jsonPath("$.email").value("alice.new@coop.com"))
                .andExpect(jsonPath("$.type").value("COMPANY"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void update_shouldReturn400_whenPayloadIsInvalid() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateAssociateRequest invalid = new UpdateAssociateRequest("", "invalid-email", null);

        mockMvc.perform(put(apiBase + "/associates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.instance").value(apiBase + "/associates/" + id))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "name", "type")));

        verifyNoInteractions(service);
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateAssociateRequest request = new UpdateAssociateRequest("Alice", "alice@coop.com", AssociateType.INDIVIDUAL);
        when(service.update(id, "Alice", "alice@coop.com", AssociateType.INDIVIDUAL))
                .thenThrow(new ResourceNotFoundException("Associado não encontrado: " + id));

        mockMvc.perform(put(apiBase + "/associates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Associado não encontrado: " + id));
    }

    @Test
    void update_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        UUID id = UUID.randomUUID();
        UpdateAssociateRequest request = new UpdateAssociateRequest("Alice", "taken@coop.com", AssociateType.INDIVIDUAL);
        when(service.update(id, "Alice", "taken@coop.com", AssociateType.INDIVIDUAL))
                .thenThrow(new ResourceConflictException("Email já cadastrado: taken@coop.com"));

        mockMvc.perform(put(apiBase + "/associates/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de recurso"))
                .andExpect(jsonPath("$.detail").value("Email já cadastrado: taken@coop.com"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(apiBase + "/associates/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Associado não encontrado: " + id))
                .when(service).delete(id);

        mockMvc.perform(delete(apiBase + "/associates/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Associado não encontrado: " + id));
    }
}
