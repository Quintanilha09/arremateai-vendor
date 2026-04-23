package com.arremateai.vendor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario", indexes = {
    @Index(name = "idx_usuario_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 500)
    private String senha;

    @Column(length = 20)
    private String telefone;

    @Column(length = 14)
    private String cpf;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoUsuario tipo = TipoUsuario.COMPRADOR;

    @Column(nullable = false)
    private Boolean ativo = true;

    // ===== CAMPOS VENDEDOR PJ =====

    @Column(length = 18, unique = true)
    private String cnpj;

    @Column(name = "razao_social", length = 300)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 300)
    private String nomeFantasia;

    @Column(name = "inscricao_estadual", length = 20)
    private String inscricaoEstadual;

    @Column(name = "email_corporativo", length = 200)
    private String emailCorporativo;

    @Column(name = "email_corporativo_verificado")
    private Boolean emailCorporativoVerificado = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_vendedor", length = 30)
    private StatusVendedor statusVendedor = StatusVendedor.PENDENTE_DOCUMENTOS;

    @Column(name = "motivo_rejeicao", columnDefinition = "TEXT")
    private String motivoRejeicao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprovado_por")
    private Usuario aprovadoPor;

    @Column(name = "aprovado_em")
    private LocalDateTime aprovadoEm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
