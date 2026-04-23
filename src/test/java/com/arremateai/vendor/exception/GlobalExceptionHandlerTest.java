package com.arremateai.vendor.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — tratamento de exceções")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ===== BusinessException =====

    @Test
    @DisplayName("Deve retornar 400 com mensagem ao tratar BusinessException")
    void deveRetornar400AoTratarBusinessException() {
        var ex = new BusinessException("Vendedor já cadastrado");

        var resposta = handler.handleBusiness(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).containsEntry("error", "Vendedor já cadastrado");
        assertThat(resposta.getBody()).containsKey("timestamp");
        assertThat(resposta.getBody()).containsKey("status");
    }

    // ===== IllegalArgumentException =====

    @Test
    @DisplayName("Deve retornar 400 com mensagem ao tratar IllegalArgumentException")
    void deveRetornar400AoTratarIllegalArgumentException() {
        var ex = new IllegalArgumentException("Argumento inválido");

        var resposta = handler.handleIllegalArgument(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).containsEntry("error", "Argumento inválido");
    }

    // ===== IllegalStateException =====

    @Test
    @DisplayName("Deve retornar 409 com mensagem ao tratar IllegalStateException")
    void deveRetornar409AoTratarIllegalStateException() {
        var ex = new IllegalStateException("Estado inválido");

        var resposta = handler.handleIllegalState(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(409);
        assertThat(resposta.getBody()).containsEntry("error", "Estado inválido");
    }

    // ===== MethodArgumentNotValidException =====

    @Test
    @DisplayName("Deve retornar 400 com primeiro campo inválido ao tratar MethodArgumentNotValidException")
    void deveRetornar400ComCampoInvalidoAoTratarValidationException() {
        var bindingResult = mock(BindingResult.class);
        var fieldError = new FieldError("objeto", "email", "não pode ser vazio");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var resposta = handler.handleValidation(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).extractingByKey("error")
                .asString().contains("email").contains("não pode ser vazio");
    }

    @Test
    @DisplayName("Deve retornar mensagem padrão quando não há field errors")
    void deveRetornarMensagemPadraoQuandoNaoHaFieldErrors() {
        var bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var resposta = handler.handleValidation(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody()).containsEntry("error", "Dados inválidos");
    }

    // ===== MaxUploadSizeExceededException =====

    @Test
    @DisplayName("Deve retornar 400 com mensagem de tamanho ao tratar MaxUploadSizeExceededException")
    void deveRetornar400AoTratarMaxUploadSizeExceededException() {
        var ex = new MaxUploadSizeExceededException(10 * 1024 * 1024L);

        var resposta = handler.handleMaxSize(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody())
                .extractingByKey("error")
                .asString()
                .contains("tamanho máximo");
    }

    // ===== Exception genérica =====

    @Test
    @DisplayName("Deve retornar 500 ao tratar exceção genérica inesperada")
    void deveRetornar500AoTratarExcecaoGenerica() {
        var ex = new RuntimeException("Erro inesperado");

        var resposta = handler.handleGeneric(ex);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody()).containsEntry("error", "Erro interno do servidor");
    }

    // ===== buildResponse — estrutura do corpo =====

    @Test
    @DisplayName("Deve incluir timestamp, status e error no corpo da resposta")
    void deveIncluirCamposObrigatoriosNoCorpoDaResposta() {
        var ex = new BusinessException("teste");

        var resposta = handler.handleBusiness(ex);

        Map<String, Object> corpo = resposta.getBody();
        assertThat(corpo).containsKeys("timestamp", "status", "error");
        assertThat(corpo.get("status")).isEqualTo(400);
    }
}
