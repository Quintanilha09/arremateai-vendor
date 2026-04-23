package com.arremateai.vendor.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprovarVendedorRequest {

    @Size(max = 1000, message = "Comentário deve ter no máximo 1000 caracteres")
    private String comentario;
}
