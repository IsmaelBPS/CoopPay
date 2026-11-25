package com.ismael.CoopPay.shared.api;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.ismael.CoopPay.shared.exception.ResourceConflictException;
import com.ismael.CoopPay.shared.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Comparator<FieldErrorDetail> FIELD_ERROR_ORDER =
            Comparator.comparing(FieldErrorDetail::field)
                    .thenComparing(FieldErrorDetail::message);

    public record FieldErrorDetail(String field, String message) {
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Recurso não encontrado: path={}, message={}", extractPath(request), ex.getMessage());
        ProblemDetail problem = problemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Object> handleResourceConflict(ResourceConflictException ex, WebRequest request) {
        log.warn("Conflito de recurso: path={}, message={}", extractPath(request), ex.getMessage());
        ProblemDetail problem = problemDetail(HttpStatus.CONFLICT, "Conflito de recurso", ex.getMessage());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String detail = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "Requisição inválida";
        log.warn("Requisição inválida: path={}, message={}", extractPath(request), detail);
        ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Requisição inválida", detail);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(
                        v.getPropertyPath().toString(),
                        v.getMessage() != null ? v.getMessage() : "Valor inválido"
                ))
                .sorted(FIELD_ERROR_ORDER)
                .toList();
        log.warn("Violação de restrição de parâmetros: path={}, errors={}", extractPath(request), errors.size());
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                "Dados de entrada inválidos",
                "Um ou mais parâmetros estão inválidos"
        );
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        log.warn("Violação de integridade de dados: path={}, cause={}",
                extractPath(request),
                cause != null ? cause.getMessage() : ex.getMessage());
        ProblemDetail problem = problemDetail(
                HttpStatus.CONFLICT,
                "Conflito de integridade de dados",
                "A operação viola uma restrição de integridade dos dados"
        );
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(
                        fe.getField(),
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido"
                ))
                .sorted(FIELD_ERROR_ORDER)
                .toList();
        log.warn("Validação de payload falhou: path={}, errors={}", extractPath(request), errors.size());
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                "Dados de entrada inválidos",
                "Um ou mais campos estão inválidos"
        );
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Corpo da requisição ilegível: path={}, cause={}", extractPath(request), ex.getMessage());
        ProblemDetail problem = problemDetail(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido",
                "Não foi possível ler o corpo da requisição. Verifique o formato JSON e os valores informados."
        );
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String paramName = (ex instanceof MethodArgumentTypeMismatchException mismatch)
                ? mismatch.getName()
                : ex.getPropertyName();
        String detail = (paramName != null && !paramName.isBlank())
                ? String.format("Parâmetro '%s' inválido: '%s'", paramName, ex.getValue())
                : "Parâmetro da requisição com formato inválido";
        log.warn("Tipo de parâmetro inválido: path={}, param={}, value={}",
                extractPath(request), paramName, ex.getValue());
        ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        log.error("Erro inesperado ao processar requisição: path={}", extractPath(request), ex);
        ProblemDetail problem = problemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor",
                "Ocorreu um erro inesperado. Tente novamente mais tarde."
        );
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ProblemDetail problem) {
            enrich(problem, request);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

    private void enrich(ProblemDetail problem, WebRequest request) {
        if (problem.getInstance() == null) {
            String path = extractPath(request);
            if (path != null && !path.isBlank()) {
                problem.setInstance(URI.create(path));
            }
        }
        if (problem.getProperties() == null || !problem.getProperties().containsKey("timestamp")) {
            problem.setProperty("timestamp", Instant.now());
        }
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            problem.setProperty("traceId", traceId);
        }
    }

    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}
