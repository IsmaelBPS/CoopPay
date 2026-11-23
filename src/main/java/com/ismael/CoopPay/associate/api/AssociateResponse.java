package com.ismael.CoopPay.associate.api;

import java.util.UUID;

import com.ismael.CoopPay.associate.domain.Associate;
import com.ismael.CoopPay.associate.domain.AssociateType;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssociateResponse(
    @Schema(description = "Identificador único do associado")
    UUID id,
    @Schema(example = "Maria Silva")
    String name,
    @Schema(example = "maria@cooppay.com")
    String email,
    @Schema(description = "Tipo do associado")
    AssociateType type
) {
    public static AssociateResponse from(Associate associate) {
        return new AssociateResponse(associate.getId(), associate.getName(), associate.getEmail(), associate.getType());
    }
}
