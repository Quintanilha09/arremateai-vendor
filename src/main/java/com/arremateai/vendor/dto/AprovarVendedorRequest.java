package com.arremateai.vendor.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AprovarVendedorRequest {

    @Size(max = 1000, message = "Comentário deve ter no máximo 1000 caracteres")
    private String comentario;
}
