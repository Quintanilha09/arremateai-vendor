package com.leilao.vendor.service;

import com.leilao.vendor.domain.CodigoVerificacao;
import com.leilao.vendor.repository.CodigoVerificacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificacaoServiceTest {

    @Mock
    private CodigoVerificacaoRepository codigoRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificacaoService verificacaoService;

    private static final String EMAIL = "contato@empresa.com";
    private static final String CODIGO = "654321";
    private static final String DADOS_JSON = "{\"nome\":\"Teste\"}";

    private void configurarExpiracao() {
        ReflectionTestUtils.setField(verificacaoService, "expirationMinutes", 10);
    }

    private CodigoVerificacao criarCodigoValido() {
        var cv = new CodigoVerificacao();
        cv.setEmail(EMAIL.toLowerCase());
        cv.setCodigo(CODIGO);
        cv.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        cv.setVerificado(false);
        cv.setDadosCadastro(DADOS_JSON);
        return cv;
    }

    // ===== enviarCodigoVerificacao =====

    @Test
    @DisplayName("Deve enviar codigo de verificacao com sucesso")
    void deveEnviarCodigoDeVerificacaoComSucesso() {
        configurarExpiracao();

        verificacaoService.enviarCodigoVerificacao(EMAIL, DADOS_JSON);

        verify(codigoRepository).deleteByEmail(EMAIL.trim().toLowerCase());
        var captor = ArgumentCaptor.forClass(CodigoVerificacao.class);
        verify(codigoRepository).save(captor.capture());
        var salvo = captor.getValue();
        assertThat(salvo.getEmail()).isEqualTo(EMAIL.trim().toLowerCase());
        assertThat(salvo.getCodigo()).hasSize(6);
        assertThat(salvo.getVerificado()).isFalse();
        assertThat(salvo.getDadosCadastro()).isEqualTo(DADOS_JSON);
        verify(emailService).enviarCodigoVerificacao(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("Deve normalizar email ao enviar codigo")
    void deveNormalizarEmailAoEnviarCodigo() {
        configurarExpiracao();

        verificacaoService.enviarCodigoVerificacao("  EMAIL@EMPRESA.COM  ", DADOS_JSON);

        verify(codigoRepository).deleteByEmail("email@empresa.com");
    }

    // ===== verificarCodigo =====

    @Test
    @DisplayName("Deve verificar codigo com sucesso")
    void deveVerificarCodigoComSucesso() {
        var cv = criarCodigoValido();
        when(codigoRepository.findByEmailAndCodigoAndVerificadoFalse(anyString(), anyString()))
                .thenReturn(Optional.of(cv));

        var resultado = verificacaoService.verificarCodigo(EMAIL, CODIGO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getVerificado()).isTrue();
        verify(codigoRepository).save(cv);
    }

    @Test
    @DisplayName("Deve retornar nulo quando codigo for invalido")
    void deveRetornarNuloQuandoCodigoForInvalido() {
        when(codigoRepository.findByEmailAndCodigoAndVerificadoFalse(anyString(), anyString()))
                .thenReturn(Optional.empty());

        var resultado = verificacaoService.verificarCodigo(EMAIL, "000000");

        assertThat(resultado).isNull();
        verify(codigoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar nulo quando codigo estiver expirado")
    void deveRetornarNuloQuandoCodigoEstiverExpirado() {
        var cv = criarCodigoValido();
        cv.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        when(codigoRepository.findByEmailAndCodigoAndVerificadoFalse(anyString(), anyString()))
                .thenReturn(Optional.of(cv));

        var resultado = verificacaoService.verificarCodigo(EMAIL, CODIGO);

        assertThat(resultado).isNull();
    }
}
