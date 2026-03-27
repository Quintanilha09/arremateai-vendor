package com.leilao.vendor.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documento_vendedor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoVendedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoDocumento tipo;

    @Column(name = "nome_arquivo", nullable = false, length = 500)
    private String nomeArquivo;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusDocumento status = StatusDocumento.PENDENTE;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analisado_por")
    private Usuario analisadoPor;

    @Column(name = "analisado_em")
    private LocalDateTime analisadoEm;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
