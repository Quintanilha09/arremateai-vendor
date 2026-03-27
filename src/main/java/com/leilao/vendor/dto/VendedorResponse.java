package com.leilao.vendor.dto;

import com.leilao.vendor.domain.StatusVendedor;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendedorResponse {
    private UUID id;
    private String nome;
    private String email;
    private String cnpj;
    private String razaoSocial;
    private String nomeFantasia;
    private String inscricaoEstadual;
    private String emailCorporativo;
    private Boolean emailCorporativoVerificado;
    private StatusVendedor statusVendedor;
    private String motivoRejeicao;
    private LocalDateTime aprovadoEm;
    private String nomeAprovadoPor;
    private LocalDateTime createdAt;
}
