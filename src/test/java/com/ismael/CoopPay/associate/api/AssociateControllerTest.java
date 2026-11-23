package com.ismael.CoopPay.associate.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
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
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
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
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(apiBase + "/associates/{id}", id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }
}
