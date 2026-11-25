package com.ismael.CoopPay.associate.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ismael.CoopPay.associate.AssociateService;
import com.ismael.CoopPay.associate.domain.Associate;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Associados", description = "Gerenciamento de associados")
@RestController
@RequestMapping("${app.routes.api-base}/associates")
@RequiredArgsConstructor
public class AssociateController {

    private final AssociateService service;

    @Operation(summary = "Busca associado por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Associado encontrado"),
        @ApiResponse(responseCode = "400", description = "UUID inválido"),
        @ApiResponse(responseCode = "404", description = "Associado não encontrado")
    })
    @GetMapping("/{id}")
    public AssociateResponse getAssociate(@PathVariable UUID id) {
        return AssociateResponse.from(service.findById(id));
    }

    @Operation(summary = "Cadastra um associado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Associado cadastrado"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    @PostMapping
    public ResponseEntity<AssociateResponse> create(@Valid @RequestBody CreateAssociateRequest body) {
        Associate created = service.create(body.name(), body.email(), body.password(), body.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(AssociateResponse.from(created));
    }

    @Operation(summary = "Atualiza um associado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Associado atualizado"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada ou UUID inválidos"),
        @ApiResponse(responseCode = "404", description = "Associado não encontrado"),
        @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    })
    @PutMapping("/{id}")
    public AssociateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAssociateRequest body) {
        Associate updated = service.update(id, body.name(), body.email(), body.type());
        return AssociateResponse.from(updated);
    }

    @Operation(summary = "Remove um associado")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Associado removido"),
        @ApiResponse(responseCode = "400", description = "UUID inválido"),
        @ApiResponse(responseCode = "404", description = "Associado não encontrado")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
