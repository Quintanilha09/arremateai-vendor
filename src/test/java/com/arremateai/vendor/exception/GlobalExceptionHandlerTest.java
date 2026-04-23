package com.arremateai.vendor.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@DisplayName("GlobalExceptionHandler — respostas RFC 7807 (ProblemDetail)")
class GlobalExceptionHandlerTest {

    private static final String URI_TESTE = "/api/vendors/123";
    private static final String TIPO_PREFIXO = "urn:arremateai:error:";

    private GlobalExceptionHandler handler;
    private HttpServletRequest requisicao;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        requisicao = mock(HttpServletRequest.class);
        when(requisicao.getRequestURI()).thenReturn(URI_TESTE);
    }

    @Test
    @DisplayName("handleBusiness → 400 business")
    void handleBusinessDeveRetornar400ComTipoBusiness() {
        ProblemDetail problema = handler.handleBusiness(
                new BusinessException("Vendedor já cadastrado"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getTitle()).isEqualTo("Regra de negócio violada");
        assertThat(problema.getDetail()).isEqualTo("Vendedor já cadastrado");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "business");
        assertThat(problema.getInstance().toString()).isEqualTo(URI_TESTE);
        assertThat(problema.getProperties()).containsKeys("timestamp", "path");
        assertThat(problema.getProperties()).containsEntry("path", URI_TESTE);
    }

    @Test
    @DisplayName("handleIllegalArgument → 400 illegal-argument")
    void handleIllegalArgumentDeveRetornar400ComTipoIllegalArgument() {
        ProblemDetail problema = handler.handleIllegalArgument(
                new IllegalArgumentException("argumento inválido"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getTitle()).isEqualTo("Argumento inválido");
        assertThat(problema.getDetail()).isEqualTo("argumento inválido");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "illegal-argument");
    }

    @Test
    @DisplayName("handleIllegalState → 409 conflict")
    void handleIllegalStateDeveRetornar409ComTipoConflict() {
        ProblemDetail problema = handler.handleIllegalState(
                new IllegalStateException("estado inválido"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problema.getTitle()).isEqualTo("Operação em conflito com o estado atual");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "conflict");
    }

    @Test
    @DisplayName("handleEntityNotFound → 404 not-found")
    void handleEntityNotFoundDeveRetornar404ComTipoNotFound() {
        ProblemDetail problema = handler.handleEntityNotFound(
                new EntityNotFoundException("Vendor 123 não existe"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problema.getTitle()).isEqualTo("Recurso não encontrado");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "not-found");
        assertThat(problema.getDetail()).isEqualTo("Vendor 123 não existe");
    }

    @Test
    @DisplayName("handleValidation → 400 validation com lista de errors[]")
    @SuppressWarnings("unchecked")
    void handleValidationDeveRetornar400ComErrosDeCampo() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError campoEmail = new FieldError("objeto", "email", "não pode ser vazio");
        FieldError campoNome = new FieldError("objeto", "nome", "obrigatório");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(campoEmail, campoNome));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problema = handler.handleValidation(ex, requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getTitle()).isEqualTo("Dados de entrada inválidos");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "validation");

        List<Map<String, String>> erros = (List<Map<String, String>>) problema.getProperties().get("errors");
        assertThat(erros).hasSize(2);
        assertThat(erros.get(0)).containsEntry("field", "email").containsEntry("message", "não pode ser vazio");
        assertThat(erros.get(1)).containsEntry("field", "nome").containsEntry("message", "obrigatório");
    }

    @Test
    @DisplayName("handleValidation → usa mensagem padrão quando defaultMessage é nulo")
    @SuppressWarnings("unchecked")
    void handleValidationDeveUsarMensagemPadraoQuandoDefaultMessageNulo() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError semMensagem = new FieldError("objeto", "campo", null);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(semMensagem));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problema = handler.handleValidation(ex, requisicao);

        List<Map<String, String>> erros = (List<Map<String, String>>) problema.getProperties().get("errors");
        assertThat(erros).hasSize(1);
        assertThat(erros.get(0)).containsEntry("message", "inválido");
    }

    @Test
    @DisplayName("handleConstraintViolation → 400 validation com errors[]")
    @SuppressWarnings("unchecked")
    void handleConstraintViolationDeveRetornar400ComErros() {
        ConstraintViolation<?> violacao = mock(ConstraintViolation.class);
        Path caminho = mock(Path.class);
        when(caminho.toString()).thenReturn("metodo.arg0.email");
        when(violacao.getPropertyPath()).thenReturn(caminho);
        when(violacao.getMessage()).thenReturn("formato inválido");

        ConstraintViolationException ex = new ConstraintViolationException("violação", Set.of(violacao));

        ProblemDetail problema = handler.handleConstraintViolation(ex, requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "validation");
        List<Map<String, String>> erros = (List<Map<String, String>>) problema.getProperties().get("errors");
        assertThat(erros).hasSize(1);
        assertThat(erros.get(0)).containsEntry("field", "metodo.arg0.email")
                .containsEntry("message", "formato inválido");
    }

    @Test
    @DisplayName("handleConstraintViolation → usa mensagem padrão quando violação sem mensagem")
    @SuppressWarnings("unchecked")
    void handleConstraintViolationDeveUsarMensagemPadraoQuandoNula() {
        ConstraintViolation<?> violacao = mock(ConstraintViolation.class);
        Path caminho = mock(Path.class);
        when(caminho.toString()).thenReturn("campo");
        when(violacao.getPropertyPath()).thenReturn(caminho);
        when(violacao.getMessage()).thenReturn(null);

        ConstraintViolationException ex = new ConstraintViolationException("v", Set.of(violacao));

        ProblemDetail problema = handler.handleConstraintViolation(ex, requisicao);

        List<Map<String, String>> erros = (List<Map<String, String>>) problema.getProperties().get("errors");
        assertThat(erros).hasSize(1);
        assertThat(erros.get(0)).containsEntry("message", "inválido");
    }

    @Test
    @DisplayName("handleMissingHeader → 401 unauthenticated quando header começa com X-User-")
    void handleMissingHeaderDeveRetornar401QuandoHeaderIdentidadeAusente() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("X-User-Id");
        when(ex.getMessage()).thenReturn("Required header 'X-User-Id' is not present.");

        ProblemDetail problema = handler.handleMissingHeader(ex, requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problema.getTitle()).isEqualTo("Autenticação necessária");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "unauthenticated");
        assertThat(problema.getDetail()).contains("X-User-Id");
    }

    @Test
    @DisplayName("handleMissingHeader → 400 illegal-argument quando header comum ausente")
    void handleMissingHeaderDeveRetornar400QuandoHeaderComumAusente() {
        MissingRequestHeaderException ex = mock(MissingRequestHeaderException.class);
        when(ex.getHeaderName()).thenReturn("Content-Type");
        when(ex.getMessage()).thenReturn("Required header 'Content-Type' is not present.");

        ProblemDetail problema = handler.handleMissingHeader(ex, requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "illegal-argument");
    }

    @Test
    @DisplayName("handleAuthentication → 401 authentication sem vazar detalhe")
    void handleAuthenticationDeveRetornar401() {
        ProblemDetail problema = handler.handleAuthentication(
                new BadCredentialsException("senha errada interna"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problema.getTitle()).isEqualTo("Credenciais inválidas");
        assertThat(problema.getDetail()).isEqualTo("Credenciais inválidas");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "authentication");
    }

    @Test
    @DisplayName("handleAccessDenied → 403 authorization")
    void handleAccessDeniedDeveRetornar403() {
        ProblemDetail problema = handler.handleAccessDenied(
                new AccessDeniedException("negado"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problema.getTitle()).isEqualTo("Acesso negado");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "authorization");
    }

    @Test
    @DisplayName("handleMaxUploadSize → 413 payload-too-large")
    void handleMaxUploadSizeDeveRetornar413() {
        ProblemDetail problema = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(10 * 1024 * 1024L), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE.value());
        assertThat(problema.getTitle()).isEqualTo("Arquivo excede o tamanho máximo permitido");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "payload-too-large");
    }

    @Test
    @DisplayName("handleGeneric → 500 internal com detalhe padrão sem vazar stack")
    void handleGenericDeveRetornar500SemVazarDetalheInterno() {
        ProblemDetail problema = handler.handleGeneric(
                new RuntimeException("NullPointerException em linha 42"), requisicao);

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problema.getTitle()).isEqualTo("Erro interno do servidor");
        assertThat(problema.getDetail()).isEqualTo("Erro interno do servidor");
        assertThat(problema.getType().toString()).isEqualTo(TIPO_PREFIXO + "internal");
        assertThat(problema.getDetail()).doesNotContain("NullPointerException");
    }

    @Test
    @DisplayName("construirProblema → deve aceitar detalhe nulo sem lançar NPE")
    void construirProblemaDeveAceitarDetalheNulo() {
        ProblemDetail problema = handler.handleBusiness(new BusinessException(null), requisicao);

        assertThat(problema).isNotNull();
        assertThat(problema.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
