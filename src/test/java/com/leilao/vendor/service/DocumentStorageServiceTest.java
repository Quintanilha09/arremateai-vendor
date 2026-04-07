package com.leilao.vendor.service;

import com.leilao.vendor.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentStorageServiceTest {

    private DocumentStorageService documentStorageService;
    private Path tempDir;

    private static final String BASE_URL = "http://localhost:8084";

    @BeforeEach
    void setUp() throws IOException {
        documentStorageService = new DocumentStorageService();
        tempDir = Files.createTempDirectory("test-docs");
        ReflectionTestUtils.setField(documentStorageService, "storagePath", tempDir.toString());
        ReflectionTestUtils.setField(documentStorageService, "baseUrl", BASE_URL);
        documentStorageService.init();
    }

    private MultipartFile criarArquivoMock(String nome, String contentType, long tamanho) throws IOException {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn(nome);
        when(arquivo.getContentType()).thenReturn(contentType);
        when(arquivo.getSize()).thenReturn(tamanho);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getInputStream()).thenReturn(new ByteArrayInputStream("conteudo".getBytes()));
        return arquivo;
    }

    @Test
    @DisplayName("Deve salvar documento PDF com sucesso")
    void deveSalvarDocumentoPdfComSucesso() throws IOException {
        var arquivo = criarArquivoMock("contrato.pdf", "application/pdf", 1024L);

        var resultado = documentStorageService.salvarDocumento(arquivo);

        assertThat(resultado).startsWith(BASE_URL + "/api/vendedores/documentos/arquivo/");
        assertThat(resultado).endsWith(".pdf");
    }

    @Test
    @DisplayName("Deve salvar documento JPG com sucesso")
    void deveSalvarDocumentoJpgComSucesso() throws IOException {
        var arquivo = criarArquivoMock("foto.jpg", "image/jpeg", 2048L);

        var resultado = documentStorageService.salvarDocumento(arquivo);

        assertThat(resultado).endsWith(".jpg");
    }

    @Test
    @DisplayName("Deve salvar documento PNG com sucesso")
    void deveSalvarDocumentoPngComSucesso() throws IOException {
        var arquivo = criarArquivoMock("doc.png", "image/png", 512L);

        var resultado = documentStorageService.salvarDocumento(arquivo);

        assertThat(resultado).endsWith(".png");
    }

    @Test
    @DisplayName("Deve lancar excecao quando arquivo for nulo")
    void deveLancarExcecaoQuandoArquivoForNulo() {
        assertThatThrownBy(() -> documentStorageService.salvarDocumento(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    @DisplayName("Deve lancar excecao quando arquivo for vazio")
    void deveLancarExcecaoQuandoArquivoForVazio() {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    @DisplayName("Deve lancar excecao quando arquivo exceder 10MB")
    void deveLancarExcecaoQuandoArquivoExceder10MB() {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getSize()).thenReturn(11L * 1024 * 1024);

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10MB");
    }

    @Test
    @DisplayName("Deve lancar excecao quando tipo de arquivo nao for permitido")
    void deveLancarExcecaoQuandoTipoDeArquivoNaoForPermitido() {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/zip");

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tipo de arquivo não permitido");
    }

    @Test
    @DisplayName("Deve lancar excecao quando content type for nulo")
    void deveLancarExcecaoQuandoContentTypeForNulo() {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tipo de arquivo não permitido");
    }

    @Test
    @DisplayName("Deve lancar excecao quando extensao nao for permitida")
    void deveLancarExcecaoQuandoExtensaoNaoForPermitida() {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(arquivo.getOriginalFilename()).thenReturn("arquivo.exe");

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Extensão não permitida");
    }

    @Test
    @DisplayName("Deve lancar excecao quando ocorrer erro de IO ao salvar")
    void deveLancarExcecaoQuandoOcorrerErroDeIOAoSalvar() throws IOException {
        var arquivo = mock(MultipartFile.class);
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(arquivo.getOriginalFilename()).thenReturn("doc.pdf");
        when(arquivo.getInputStream()).thenThrow(new IOException("Erro de leitura"));

        assertThatThrownBy(() -> documentStorageService.salvarDocumento(arquivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Erro ao salvar arquivo");
    }
}
