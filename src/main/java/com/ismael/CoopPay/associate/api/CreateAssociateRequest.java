package com.ismael.CoopPay.associate.api;

import com.ismael.CoopPay.associate.domain.AssociateType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAssociateRequest(
    @Schema(description = "Nome completo do associado", example = "Maria Silva")
    @NotBlank String name,
    @Schema(description = "E-mail único do associado", example = "maria@cooppay.com")
    @NotBlank @Email String email,
    @Schema(description = "Senha de acesso", example = "senha-segura")
    @NotBlank String password,
    @Schema(description = "Tipo do associado", example = "INDIVIDUAL")
    @NotNull AssociateType type
) {}
