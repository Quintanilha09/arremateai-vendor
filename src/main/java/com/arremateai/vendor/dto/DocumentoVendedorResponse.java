package com.arremateai.vendor.dto;

import com.arremateai.vendor.domain.StatusDocumento;
import com.arremateai.vendor.domain.TipoDocumento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoVendedorResponse {
    private UUID id;
    private TipoDocumento tipo;
    private String tipoDescricao;
    private String nomeArquivo;
    private String url;
    private Long tamanhoBytes;
    private String mimeType;
    private StatusDocumento status;
    private String motivoRejeicao;
    private String nomeAnalisadoPor;
    private LocalDateTime analisadoEm;
    private LocalDateTime createdAt;
}
