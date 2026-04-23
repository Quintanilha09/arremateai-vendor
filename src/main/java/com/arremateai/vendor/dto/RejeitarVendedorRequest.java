package com.arremateai.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejeitarVendedorRequest {

    @NotBlank(message = "Motivo é obrigatório")
    @Size(min = 10, max = 1000, message = "Motivo deve ter entre 10 e 1000 caracteres")
    private String motivo;
}
