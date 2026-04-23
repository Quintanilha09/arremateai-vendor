package com.arremateai.vendor.controller;

import com.arremateai.vendor.config.RateLimitConfig;
import com.arremateai.vendor.domain.StatusVendedor;
import com.arremateai.vendor.domain.TipoDocumento;
import com.arremateai.vendor.dto.*;
import com.arremateai.vendor.service.VendedorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendedorControllerTest {

    @TempDir
    Path diretorioTemp;

    @Mock
    private VendedorService vendedorService;

    @Mock
    private RateLimitConfig rateLimitConfig;

    @InjectMocks
    private VendedorController vendedorController;

    private static final String USER_ID_PADRAO = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID USER_UUID = UUID.fromString(USER_ID_PADRAO);

    private VendedorResponse criarVendedorResponse() {
        return VendedorResponse.builder()
                .id(USER_UUID)
                .nome("Empresa Teste")
                .email("empresa@teste.com")
                .cnpj("12345678000199")
                .statusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS)
                .build();
    }

    private CadastroVendedorRequest criarCadastroRequest() {
        return CadastroVendedorRequest.builder()
                .nome("Empresa Teste")
                .email("empresa@teste.com")
                .senha("Senha@123")
                .cnpj("12345678000199")
                .razaoSocial("Empresa Teste LTDA")
                .emailCorporativo("contato@empresa.com")
                .build();
    }

    // ===== registrar =====

    @Test
    @DisplayName("Deve registrar vendedor e retornar mensagem de sucesso")
    void deveRegistrarVendedorERetornarMensagem() {
        when(vendedorService.registrarVendedor(any())).thenReturn(Map.of("message", "Cadastro realizado"));

        var resultado = vendedorController.registrar(criarCadastroRequest());

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).containsKey("message");
        verify(vendedorService).registrarVendedor(any());
    }

    // ===== verificarEmailCorporativo =====

    @Test
    @DisplayName("Deve verificar email corporativo com sucesso")
    void deveVerificarEmailCorporativoComSucesso() {
        when(rateLimitConfig.tentativaPermitida(any())).thenReturn(true);
        when(vendedorService.verificarEmailCorporativo("contato@empresa.com", "123456"))
                .thenReturn(criarVendedorResponse());

        var resultado = vendedorController.verificarEmailCorporativo("contato@empresa.com", "123456");

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar 429 quando rate limit excedido na verificacao")
    void deveRetornar429QuandoRateLimitExcedido() {
        when(rateLimitConfig.tentativaPermitida(any())).thenReturn(false);

        var resultado = vendedorController.verificarEmailCorporativo("contato@empresa.com", "123456");

        assertThat(resultado.getStatusCode().value()).isEqualTo(429);
        verifyNoInteractions(vendedorService);
    }

    // ===== reenviarCodigo =====

    @Test
    @DisplayName("Deve reenviar código de verificação")
    void deveReenviarCodigoDeVerificacao() {
        when(rateLimitConfig.tentativaPermitida(any())).thenReturn(true);
        doNothing().when(vendedorService).reenviarCodigoVerificacao("contato@empresa.com");

        var resultado = vendedorController.reenviarCodigo("contato@empresa.com");

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).containsEntry("message", "Código reenviado com sucesso.");
    }

    // ===== buscarMeuStatus =====

    @Test
    @DisplayName("Deve retornar status do vendedor autenticado")
    void deveRetornarStatusDoVendedorAutenticado() {
        when(vendedorService.buscarMeuStatus(USER_UUID)).thenReturn(criarVendedorResponse());

        var resultado = vendedorController.buscarMeuStatus(USER_ID_PADRAO);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody().getStatusVendedor()).isEqualTo(StatusVendedor.PENDENTE_DOCUMENTOS);
    }

    // ===== listarMeusDocumentos =====

    @Test
    @DisplayName("Deve retornar lista de documentos do vendedor")
    void deveRetornarListaDeDocumentosDoVendedor() {
        var doc = DocumentoVendedorResponse.builder()
                .id(UUID.randomUUID())
                .tipo(TipoDocumento.CNPJ_RECEITA)
                .nomeArquivo("cnpj.pdf")
                .build();
        when(vendedorService.listarMeusDocumentos(USER_UUID)).thenReturn(List.of(doc));

        var resultado = vendedorController.listarMeusDocumentos(USER_ID_PADRAO);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).hasSize(1);
    }

    // ===== uploadDocumento =====

    @Test
    @DisplayName("Deve fazer upload de documento com sucesso")
    void deveFazerUploadDeDocumentoComSucesso() {
        var arquivo = new MockMultipartFile("arquivo", "doc.pdf", "application/pdf", "conteudo".getBytes());
        var resposta = DocumentoVendedorResponse.builder()
                .id(UUID.randomUUID())
                .tipo(TipoDocumento.CNPJ_RECEITA)
                .build();
        when(vendedorService.uploadDocumento(eq(USER_UUID), eq(TipoDocumento.CNPJ_RECEITA), any())).thenReturn(resposta);

        var resultado = vendedorController.uploadDocumento(USER_ID_PADRAO, TipoDocumento.CNPJ_RECEITA, arquivo);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        verify(vendedorService).uploadDocumento(eq(USER_UUID), eq(TipoDocumento.CNPJ_RECEITA), any());
    }

    // ===== servirDocumento =====

    @Test
    @DisplayName("Deve retornar 401 quando userId nao fornecido")
    void deveRetornar401QuandoUserIdNaoFornecido() {
        ReflectionTestUtils.setField(vendedorController, "storagePath", System.getProperty("java.io.tmpdir"));

        var resultado = vendedorController.servirDocumento("arquivo.pdf", null, null);

        assertThat(resultado.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Deve retornar 403 para path traversal")
    void deveRetornar403ParaPathTraversal() {
        ReflectionTestUtils.setField(vendedorController, "storagePath", System.getProperty("java.io.tmpdir"));

        var resultado = vendedorController.servirDocumento("../../etc/passwd", USER_ID_PADRAO, "ADMIN");

        assertThat(resultado.getStatusCode().value()).isIn(403, 404);
    }

    @Test
    @DisplayName("Deve retornar 200 e recurso quando arquivo existe e usuario e admin")
    void deveRetornar200ERecursoQuandoArquivoExiste() throws IOException {
        var arquivo = Files.createTempFile(diretorioTemp, "documento", ".pdf");
        Files.writeString(arquivo, "conteudo simulado do pdf");
        ReflectionTestUtils.setField(vendedorController, "storagePath", diretorioTemp.toString());

        var resultado = vendedorController.servirDocumento(arquivo.getFileName().toString(), USER_ID_PADRAO, "ADMIN");

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).isNotNull();
        assertThat(resultado.getHeaders().getContentDisposition().toString())
                .contains("inline");
    }

    @Test
    @DisplayName("Deve retornar 404 quando documento nao existe e vendedor tem acesso")
    void deveRetornar404QuandoDocumentoNaoExiste() {
        ReflectionTestUtils.setField(vendedorController, "storagePath", System.getProperty("java.io.tmpdir"));
        when(vendedorService.verificarAcessoArquivo(any(), any())).thenReturn(true);

        var resultado = vendedorController.servirDocumento("arquivo-inexistente.pdf", USER_ID_PADRAO, "VENDEDOR");

        assertThat(resultado.getStatusCode().value()).isEqualTo(404);
    }
}
