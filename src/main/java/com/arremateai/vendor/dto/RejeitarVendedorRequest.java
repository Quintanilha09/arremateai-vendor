package com.arremateai.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejeitarVendedorRequest {

    @NotBlank(message = "Motivo é obrigatório")
    @Size(min = 10, max = 1000, message = "Motivo deve ter entre 10 e 1000 caracteres")
    private String motivo;
}
