package com.leilao.vendor.domain;

public enum TipoDocumento {
    CNPJ_RECEITA("Comprovante CNPJ"),
    CONTRATO_SOCIAL("Contrato Social"),
    MEI("Certificado MEI"),
    RG_RESPONSAVEL("RG do Responsável"),
    CNH_RESPONSAVEL("CNH do Responsável"),
    COMPROVANTE_ENDERECO("Comprovante de Endereço"),
    CRECI("Registro CRECI");

    private final String descricao;

    TipoDocumento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
