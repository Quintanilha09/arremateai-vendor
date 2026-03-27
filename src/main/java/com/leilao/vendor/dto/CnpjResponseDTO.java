package com.leilao.vendor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CnpjResponseDTO {

    private String cnpj;

    @JsonProperty("razao_social")
    private String razaoSocial;

    @JsonProperty("nome_fantasia")
    private String nomeFantasia;

    private String situacao;

    @JsonProperty("data_situacao")
    private String dataSituacao;

    private String uf;
    private String municipio;

    @Data
    public static class AtividadeEconomica {
        private String code;
        private String text;
    }

    @JsonProperty("atividade_principal")
    private List<AtividadeEconomica> atividadePrincipal;

    public boolean isAtivo() {
        return "ATIVA".equalsIgnoreCase(this.situacao);
    }
}
