package com.leilao.vendor.service;

import com.leilao.vendor.dto.CnpjResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CnpjValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CnpjValidationService cnpjValidationService;

    private static final String CNPJ_VALIDO = "12345678000199";
    private static final String CNPJ_FORMATADO = "12.345.678/0001-99";

    private CnpjResponseDTO criarRespostaAtiva() {
        var response = new CnpjResponseDTO();
        response.setCnpj(CNPJ_VALIDO);
        response.setRazaoSocial("Empresa Teste LTDA");
        response.setSituacao("ATIVA");
        return response;
    }

    private CnpjResponseDTO criarRespostaInativa() {
        var response = new CnpjResponseDTO();
        response.setCnpj(CNPJ_VALIDO);
        response.setRazaoSocial("Empresa Inativa LTDA");
        response.setSituacao("BAIXADA");
        return response;
    }

    @Test
    @DisplayName("Deve validar CNPJ ativo com sucesso")
    void deveValidarCnpjAtivoComSucesso() {
        when(restTemplate.getForObject(anyString(), eq(CnpjResponseDTO.class)))
                .thenReturn(criarRespostaAtiva());

        var resultado = cnpjValidationService.validarCnpj(CNPJ_VALIDO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getRazaoSocial()).isEqualTo("Empresa Teste LTDA");
        assertThat(resultado.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve validar CNPJ formatado removendo caracteres especiais")
    void deveValidarCnpjFormatadoRemovendoCaracteresEspeciais() {
        when(restTemplate.getForObject(anyString(), eq(CnpjResponseDTO.class)))
                .thenReturn(criarRespostaAtiva());

        var resultado = cnpjValidationService.validarCnpj(CNPJ_FORMATADO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve lancar excecao quando CNPJ for inativo")
    void deveLancarExcecaoQuandoCnpjForInativo() {
        when(restTemplate.getForObject(anyString(), eq(CnpjResponseDTO.class)))
                .thenReturn(criarRespostaInativa());

        assertThatThrownBy(() -> cnpjValidationService.validarCnpj(CNPJ_VALIDO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("situação: BAIXADA");
    }

    @Test
    @DisplayName("Deve lancar excecao quando CNPJ tiver menos de 14 digitos")
    void deveLancarExcecaoQuandoCnpjTiverMenosDe14Digitos() {
        assertThatThrownBy(() -> cnpjValidationService.validarCnpj("123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 dígitos");
    }

    @Test
    @DisplayName("Deve lancar excecao quando resposta for nula")
    void deveLancarExcecaoQuandoRespostaForNula() {
        when(restTemplate.getForObject(anyString(), eq(CnpjResponseDTO.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> cnpjValidationService.validarCnpj(CNPJ_VALIDO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Erro ao consultar CNPJ");
    }

    @Test
    @DisplayName("Deve lancar excecao quando ocorrer erro na API")
    void deveLancarExcecaoQuandoOcorrerErroNaApi() {
        when(restTemplate.getForObject(anyString(), eq(CnpjResponseDTO.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThatThrownBy(() -> cnpjValidationService.validarCnpj(CNPJ_VALIDO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não foi possível validar o CNPJ");
    }
}
