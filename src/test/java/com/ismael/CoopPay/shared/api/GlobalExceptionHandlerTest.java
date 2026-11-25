package com.ismael.CoopPay.shared.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @AfterEach
    void clearMdc() {
        MDC.remove("traceId");
    }

    @Test
    void shouldReturn404ProblemDetail_whenResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Registro não localizado"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.traceId").doesNotExist());
    }

    @Test
    void shouldIncludeTraceId_whenMdcHasTraceId() throws Exception {
        MDC.put("traceId", "trace-abc-123");

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.traceId").value("trace-abc-123"));
    }

    @Test
    void shouldReturn409ProblemDetail_whenResourceConflict() throws Exception {
        mockMvc.perform(post("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de recurso"))
                .andExpect(jsonPath("$.detail").value("Email já cadastrado"))
                .andExpect(jsonPath("$.instance").value("/test/conflict"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn400ProblemDetail_whenIllegalArgumentHasMessage() throws Exception {
        mockMvc.perform(post("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("Valor deve ser positivo"))
                .andExpect(jsonPath("$.instance").value("/test/illegal-arg"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn400ProblemDetailWithFallback_whenIllegalArgumentHasBlankMessage() throws Exception {
        mockMvc.perform(post("/test/illegal-arg").param("blank", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail").value("Requisição inválida"));
    }

    @Test
    void shouldReturn400WithSortedFieldErrors_whenBeanValidationFails() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais campos estão inválidos"))
                .andExpect(jsonPath("$.instance").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[1].message").isNotEmpty());
    }

    @Test
    void shouldReturn400WithSortedFieldErrors_whenConstraintViolationOccurs() throws Exception {
        mockMvc.perform(get("/test/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.detail").value("Um ou mais parâmetros estão inválidos"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("name"));
    }

    @Test
    void shouldReturn400ProblemDetail_whenBodyIsMalformedJson() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Corpo da requisição inválido"))
                .andExpect(jsonPath("$.detail").value("Não foi possível ler o corpo da requisição. Verifique o formato JSON e os valores informados."))
                .andExpect(jsonPath("$.instance").value("/test/validate"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn400ProblemDetail_whenPathVariableTypeMismatches() throws Exception {
        mockMvc.perform(get("/test/uuid/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Parâmetro 'id' inválido: 'not-a-uuid'"))
                .andExpect(jsonPath("$.instance").value("/test/uuid/not-a-uuid"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn400ProblemDetailWithFallback_whenTypeMismatchHasNoPropertyName() throws Exception {
        mockMvc.perform(get("/test/generic-type-mismatch"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("Parâmetro da requisição com formato inválido"));
    }

    @Test
    void shouldReturn409WithoutLeakingSql_whenDataIntegrityViolationOccurs() throws Exception {
        mockMvc.perform(post("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflito de integridade de dados"))
                .andExpect(jsonPath("$.detail").value("A operação viola uma restrição de integridade dos dados"))
                .andExpect(content().string(not(containsString("uk_associates_email"))))
                .andExpect(content().string(not(containsString("duplicate key"))));
    }

    @Test
    void shouldReturn405EnrichedProblemDetail_whenHttpMethodNotSupported() throws Exception {
        mockMvc.perform(post("/test/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturn500WithoutLeakingInternalDetails_whenUnexpectedExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Erro interno do servidor"))
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."))
                .andExpect(jsonPath("$.instance").value("/test/unexpected"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(not(containsString("segredo interno"))));
    }

    record ValidationPayload(
            @NotBlank(message = "Nome é obrigatório") String name,
            @Email(message = "E-mail inválido") String email
    ) {
    }

    @RestController
    static class TestController {

        private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Registro não localizado");
        }

        @PostMapping("/test/conflict")
        String conflict() {
            throw new ResourceConflictException("Email já cadastrado");
        }

        @PostMapping("/test/illegal-arg")
        String illegalArg(@RequestParam(defaultValue = "false") boolean blank) {
            throw new IllegalArgumentException(blank ? "   " : "Valor deve ser positivo");
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody ValidationPayload payload) {
            return payload.name();
        }

        @GetMapping("/test/constraint")
        String constraint() {
            Set<ConstraintViolation<ValidationPayload>> violations =
                    validator.validate(new ValidationPayload("", "invalido"));
            throw new ConstraintViolationException(violations);
        }

        @GetMapping("/test/uuid/{id}")
        String uuid(@PathVariable UUID id) {
            return id.toString();
        }

        @GetMapping("/test/generic-type-mismatch")
        String genericTypeMismatch() {
            throw new TypeMismatchException("abc", Integer.class);
        }

        @PostMapping("/test/data-integrity")
        String dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "constraint error",
                    new RuntimeException("ERROR: duplicate key value violates unique constraint uk_associates_email")
            );
        }

        @GetMapping("/test/unexpected")
        String unexpected() {
            throw new RuntimeException("segredo interno de banco");
        }
    }
}
