package com.leilao.vendor.validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailCorporativoValidatorTest {

    private final EmailCorporativoValidator validator = new EmailCorporativoValidator();

    // ===== isEmailCorporativo =====

    @Test
    @DisplayName("Deve retornar true quando email for corporativo")
    void deveRetornarTrueQuandoEmailForCorporativo() {
        assertThat(validator.isEmailCorporativo("contato@minhaempresa.com.br")).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false quando email for do Gmail")
    void deveRetornarFalseQuandoEmailForDoGmail() {
        assertThat(validator.isEmailCorporativo("usuario@gmail.com")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for do Hotmail")
    void deveRetornarFalseQuandoEmailForDoHotmail() {
        assertThat(validator.isEmailCorporativo("usuario@hotmail.com")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for do Yahoo")
    void deveRetornarFalseQuandoEmailForDoYahoo() {
        assertThat(validator.isEmailCorporativo("usuario@yahoo.com")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for do Outlook")
    void deveRetornarFalseQuandoEmailForDoOutlook() {
        assertThat(validator.isEmailCorporativo("usuario@outlook.com")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for nulo")
    void deveRetornarFalseQuandoEmailForNulo() {
        assertThat(validator.isEmailCorporativo(null)).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for vazio")
    void deveRetornarFalseQuandoEmailForVazio() {
        assertThat(validator.isEmailCorporativo("")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for apenas espacos")
    void deveRetornarFalseQuandoEmailForApenasEspacos() {
        assertThat(validator.isEmailCorporativo("   ")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email nao contiver arroba")
    void deveRetornarFalseQuandoEmailNaoContiverArroba() {
        assertThat(validator.isEmailCorporativo("emailsemarroba")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email terminar com arroba")
    void deveRetornarFalseQuandoEmailTerminarComArroba() {
        assertThat(validator.isEmailCorporativo("email@")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for do Protonmail")
    void deveRetornarFalseQuandoEmailForDoProtonmail() {
        assertThat(validator.isEmailCorporativo("usuario@protonmail.com")).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false quando email for temporario")
    void deveRetornarFalseQuandoEmailForTemporario() {
        assertThat(validator.isEmailCorporativo("usuario@mailinator.com")).isFalse();
    }

    // ===== validarEmailCorporativo =====

    @Test
    @DisplayName("Deve lancar excecao quando email for pessoal")
    void deveLancarExcecaoQuandoEmailForPessoal() {
        assertThatThrownBy(() -> validator.validarEmailCorporativo("teste@gmail.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email corporativo inválido");
    }

    @Test
    @DisplayName("Nao deve lancar excecao quando email for corporativo")
    void naoDeveLancarExcecaoQuandoEmailForCorporativo() {
        validator.validarEmailCorporativo("financeiro@empresa.com.br");
    }

    @Test
    @DisplayName("Deve lancar excecao quando email nao contiver arroba na validacao")
    void deveLancarExcecaoQuandoEmailNaoContiverArrobaNaValidacao() {
        assertThatThrownBy(() -> validator.validarEmailCorporativo("emailsemarroba"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email corporativo inválido");
    }
}
