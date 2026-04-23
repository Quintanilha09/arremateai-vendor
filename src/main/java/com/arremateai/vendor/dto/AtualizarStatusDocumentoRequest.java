package com.arremateai.vendor.dto;

import com.arremateai.vendor.domain.StatusDocumento;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarStatusDocumentoRequest {

    @NotNull(message = "Status é obrigatório")
    private StatusDocumento status;

    @Size(max = 1000, message = "Motivo deve ter no máximo 1000 caracteres")
    private String motivo;
}
