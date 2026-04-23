package com.arremateai.vendor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.arremateai.vendor.domain.CodigoVerificacao;
import com.arremateai.vendor.domain.DocumentoVendedor;
import com.arremateai.vendor.domain.HistoricoStatusVendedor;
import com.arremateai.vendor.domain.StatusDocumento;
import com.arremateai.vendor.domain.StatusVendedor;
import com.arremateai.vendor.domain.TipoDocumento;
import com.arremateai.vendor.domain.TipoUsuario;
import com.arremateai.vendor.domain.Usuario;
import com.arremateai.vendor.dto.CadastroVendedorRequest;
import com.arremateai.vendor.dto.CadastroVendedorTemp;
import com.arremateai.vendor.dto.CnpjResponseDTO;
import com.arremateai.vendor.exception.BusinessException;
import com.arremateai.vendor.repository.CodigoVerificacaoRepository;
import com.arremateai.vendor.repository.DocumentoVendedorRepository;
import com.arremateai.vendor.repository.HistoricoStatusVendedorRepository;
import com.arremateai.vendor.repository.UsuarioRepository;
import com.arremateai.vendor.validator.EmailCorporativoValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendedorServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DocumentoVendedorRepository documentoRepository;

    @Mock
    private HistoricoStatusVendedorRepository historicoRepository;

    @Mock
    private CodigoVerificacaoRepository codigoVerificacaoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailCorporativoValidator emailCorporativoValidator;

    @Mock
    private CnpjValidationService cnpjValidationService;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private VerificacaoService verificacaoService;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VendedorService vendedorService;

    private static final UUID ID_PADRAO = UUID.randomUUID();
    private static final String CNPJ_PADRAO = "12345678000199";
    private static final String EMAIL_PADRAO = "usuario@teste.com";
    private static final String EMAIL_CORP = "contato@empresa.com.br";
    private static final String SENHA_PADRAO = "Senha@123";

    private CadastroVendedorRequest criarRequestPadrao() {
        return CadastroVendedorRequest.builder()
                .nome("Empresa Teste")
                .email(EMAIL_PADRAO)
                .senha(SENHA_PADRAO)
                .cpf("12345678901")
                .telefone("11999999999")
                .cnpj(CNPJ_PADRAO)
                .razaoSocial("Empresa Teste LTDA")
                .nomeFantasia("Empresa Teste")
                .inscricaoEstadual("123456789")
                .emailCorporativo(EMAIL_CORP)
                .build();
    }

    private Usuario criarVendedorPadrao() {
        var vendedor = new Usuario();
        vendedor.setId(ID_PADRAO);
        vendedor.setNome("Empresa Teste");
        vendedor.setEmail(EMAIL_PADRAO);
        vendedor.setSenha("encodedPassword");
        vendedor.setCnpj(CNPJ_PADRAO);
        vendedor.setRazaoSocial("Empresa Teste LTDA");
        vendedor.setNomeFantasia("Empresa Teste");
        vendedor.setEmailCorporativo(EMAIL_CORP);
        vendedor.setEmailCorporativoVerificado(true);
        vendedor.setTipo(TipoUsuario.VENDEDOR);
        vendedor.setStatusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS);
        vendedor.setAtivo(true);
        return vendedor;
    }

    private CodigoVerificacao criarCodigoVerificacao(String dadosJson) {
        var cv = new CodigoVerificacao();
        cv.setEmail(EMAIL_CORP);
        cv.setCodigo("123456");
        cv.setVerificado(true);
        cv.setDadosCadastro(dadosJson);
        return cv;
    }

    private DocumentoVendedor criarDocumento(TipoDocumento tipo) {
        return DocumentoVendedor.builder()
                .id(UUID.randomUUID())
                .usuario(criarVendedorPadrao())
                .tipo(tipo)
                .nomeArquivo("documento.pdf")
                .url("http://localhost:8084/api/vendedores/documentos/arquivo/doc.pdf")
                .tamanhoBytes(1024L)
                .mimeType("application/pdf")
                .status(StatusDocumento.PENDENTE)
                .build();
    }

    // ===== registrarVendedor =====

    @Test
    @DisplayName("Deve registrar vendedor com sucesso")
    void deveRegistrarVendedorComSucesso() throws Exception {
        var request = criarRequestPadrao();
        when(usuarioRepository.existsByCnpj(CNPJ_PADRAO)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL_PADRAO)).thenReturn(false);
        when(usuarioRepository.findByEmailCorporativo(EMAIL_CORP)).thenReturn(Optional.empty());
        when(cnpjValidationService.validarCnpj(CNPJ_PADRAO)).thenReturn(new CnpjResponseDTO());
        when(passwordEncoder.encode(SENHA_PADRAO)).thenReturn("encodedPassword");
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"nome\":\"Empresa\"}");

        var resultado = vendedorService.registrarVendedor(request);

        assertThat(resultado).containsKey("message");
        assertThat(resultado).containsKey("emailCorporativo");
        assertThat(resultado.get("emailCorporativo")).isEqualTo(EMAIL_CORP);
        verify(emailCorporativoValidator).validarEmailCorporativo(EMAIL_CORP);
        verify(verificacaoService).enviarCodigoVerificacao(eq(EMAIL_CORP), anyString());
    }

    @Test
    @DisplayName("Deve lancar excecao quando CNPJ ja estiver cadastrado")
    void deveLancarExcecaoQuandoCnpjJaEstiverCadastrado() {
        var request = criarRequestPadrao();
        when(usuarioRepository.existsByCnpj(CNPJ_PADRAO)).thenReturn(true);

        assertThatThrownBy(() -> vendedorService.registrarVendedor(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    @DisplayName("Deve lancar excecao quando email ja estiver em uso")
    void deveLancarExcecaoQuandoEmailJaEstiverEmUso() {
        var request = criarRequestPadrao();
        when(usuarioRepository.existsByCnpj(CNPJ_PADRAO)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL_PADRAO)).thenReturn(true);

        assertThatThrownBy(() -> vendedorService.registrarVendedor(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email já está em uso");
    }

    @Test
    @DisplayName("Deve lancar excecao quando email corporativo ja estiver em uso")
    void deveLancarExcecaoQuandoEmailCorporativoJaEstiverEmUso() {
        var request = criarRequestPadrao();
        when(usuarioRepository.existsByCnpj(CNPJ_PADRAO)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL_PADRAO)).thenReturn(false);
        when(usuarioRepository.findByEmailCorporativo(EMAIL_CORP)).thenReturn(Optional.of(criarVendedorPadrao()));

        assertThatThrownBy(() -> vendedorService.registrarVendedor(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email corporativo já está em uso");
    }

    @Test
    @DisplayName("Deve lancar excecao quando erro ao processar cadastro")
    void deveLancarExcecaoQuandoErroAoProcessarCadastro() throws Exception {
        var request = criarRequestPadrao();
        when(usuarioRepository.existsByCnpj(CNPJ_PADRAO)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL_PADRAO)).thenReturn(false);
        when(usuarioRepository.findByEmailCorporativo(EMAIL_CORP)).thenReturn(Optional.empty());
        when(cnpjValidationService.validarCnpj(CNPJ_PADRAO)).thenReturn(new CnpjResponseDTO());
        when(passwordEncoder.encode(SENHA_PADRAO)).thenReturn("encodedPassword");
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Erro") {});

        assertThatThrownBy(() -> vendedorService.registrarVendedor(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Erro ao processar cadastro");
    }

    @Test
    @DisplayName("Deve propagar BusinessException do email corporativo validator")
    void devePropagarBusinessExceptionDoEmailCorporativoValidator() {
        var request = criarRequestPadrao();
        doThrow(new IllegalArgumentException("Email corporativo inválido"))
                .when(emailCorporativoValidator).validarEmailCorporativo(EMAIL_CORP);

        assertThatThrownBy(() -> vendedorService.registrarVendedor(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email corporativo inválido");
    }

    // ===== verificarEmailCorporativo =====

    @Test
    @DisplayName("Deve verificar email corporativo com sucesso")
    void deveVerificarEmailCorporativoComSucesso() throws Exception {
        var request = criarRequestPadrao();
        var dadosJson = "{\"nome\":\"Empresa\"}";
        var cv = criarCodigoVerificacao(dadosJson);
        var vendedorSalvo = criarVendedorPadrao();

        var tempCadastro = CadastroVendedorTemp.builder()
                .nome("Empresa Teste")
                .email(EMAIL_PADRAO)
                .senhaHash("encodedPassword")
                .cpf("12345678901")
                .telefone("11999999999")
                .cnpj(CNPJ_PADRAO)
                .razaoSocial("Empresa Teste LTDA")
                .nomeFantasia("Empresa Teste")
                .inscricaoEstadual("123456789")
                .emailCorporativo(EMAIL_CORP)
                .build();
        when(verificacaoService.verificarCodigo(EMAIL_CORP, "123456")).thenReturn(cv);
        when(objectMapper.readValue(dadosJson, CadastroVendedorTemp.class)).thenReturn(tempCadastro);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(vendedorSalvo);

        var resultado = vendedorService.verificarEmailCorporativo(EMAIL_CORP, "123456");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(ID_PADRAO);
        verify(adminNotificationService).notificarNovoVendedor(any(Usuario.class));
        verify(historicoRepository).save(any(HistoricoStatusVendedor.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando codigo de verificacao for nulo")
    void deveLancarExcecaoQuandoCodigoDeVerificacaoForNulo() {
        when(verificacaoService.verificarCodigo(EMAIL_CORP, "000000")).thenReturn(null);

        assertThatThrownBy(() -> vendedorService.verificarEmailCorporativo(EMAIL_CORP, "000000"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Código de verificação inválido");
    }

    @Test
    @DisplayName("Deve lancar excecao quando erro ao deserializar dados do cadastro")
    void deveLancarExcecaoQuandoErroAoDeserializarDadosDoCadastro() throws Exception {
        var cv = criarCodigoVerificacao("json-invalido");
        when(verificacaoService.verificarCodigo(EMAIL_CORP, "123456")).thenReturn(cv);
        when(objectMapper.readValue("json-invalido", CadastroVendedorTemp.class))
                .thenThrow(new JsonProcessingException("Erro parse") {});

        assertThatThrownBy(() -> vendedorService.verificarEmailCorporativo(EMAIL_CORP, "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Erro ao processar verificação");
    }

    // ===== reenviarCodigoVerificacao =====

    @Test
    @DisplayName("Deve reenviar codigo de verificacao com sucesso")
    void deveReenviarCodigoDeVerificacaoComSucesso() {
        var cv = criarCodigoVerificacao("{\"dados\":\"json\"}");
        when(codigoVerificacaoRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL_CORP.trim().toLowerCase()))
                .thenReturn(Optional.of(cv));

        vendedorService.reenviarCodigoVerificacao(EMAIL_CORP);

        verify(verificacaoService).enviarCodigoVerificacao(EMAIL_CORP, "{\"dados\":\"json\"}");
    }

    @Test
    @DisplayName("Deve lancar excecao quando nao existir cadastro pendente para reenvio")
    void deveLancarExcecaoQuandoNaoExistirCadastroPendenteParaReenvio() {
        when(codigoVerificacaoRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL_CORP.trim().toLowerCase()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendedorService.reenviarCodigoVerificacao(EMAIL_CORP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nenhum cadastro pendente");
    }

    @Test
    @DisplayName("Deve lancar excecao quando dados do cadastro forem nulos no reenvio")
    void deveLancarExcecaoQuandoDadosDoCadastroForemNulosNoReenvio() {
        var cv = criarCodigoVerificacao(null);
        cv.setDadosCadastro(null);
        when(codigoVerificacaoRepository.findFirstByEmailOrderByCreatedAtDesc(EMAIL_CORP.trim().toLowerCase()))
                .thenReturn(Optional.of(cv));

        assertThatThrownBy(() -> vendedorService.reenviarCodigoVerificacao(EMAIL_CORP))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Erro ao reenviar código");
    }

    // ===== buscarMeuStatus =====

    @Test
    @DisplayName("Deve retornar status do vendedor")
    void deveRetornarStatusDoVendedor() {
        var vendedor = criarVendedorPadrao();
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var resultado = vendedorService.buscarMeuStatus(ID_PADRAO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getStatusVendedor()).isEqualTo(StatusVendedor.PENDENTE_DOCUMENTOS);
    }

    // ===== listarMeusDocumentos =====

    @Test
    @DisplayName("Deve listar documentos do vendedor")
    void deveListarDocumentosDoVendedor() {
        var docs = List.of(
                criarDocumento(TipoDocumento.CNPJ_RECEITA),
                criarDocumento(TipoDocumento.CONTRATO_SOCIAL)
        );
        when(documentoRepository.findByUsuarioId(ID_PADRAO)).thenReturn(docs);

        var resultado = vendedorService.listarMeusDocumentos(ID_PADRAO);

        assertThat(resultado).hasSize(2);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nao houver documentos")
    void deveRetornarListaVaziaQuandoNaoHouverDocumentos() {
        when(documentoRepository.findByUsuarioId(ID_PADRAO)).thenReturn(List.of());

        var resultado = vendedorService.listarMeusDocumentos(ID_PADRAO);

        assertThat(resultado).isEmpty();
    }

    // ===== uploadDocumento =====

    @Test
    @DisplayName("Deve fazer upload de documento com sucesso")
    void deveFazerUploadDeDocumentoComSucesso() {
        var vendedor = criarVendedorPadrao();
        vendedor.setStatusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var arquivo = mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("contrato.pdf");
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(documentStorageService.salvarDocumento(arquivo)).thenReturn("http://localhost/doc.pdf");
        when(documentoRepository.save(any(DocumentoVendedor.class))).thenAnswer(i -> {
            DocumentoVendedor d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        var resultado = vendedorService.uploadDocumento(ID_PADRAO, TipoDocumento.CNPJ_RECEITA, arquivo);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getTipo()).isEqualTo(TipoDocumento.CNPJ_RECEITA);
        verify(documentoRepository).save(any(DocumentoVendedor.class));
    }

    @Test
    @DisplayName("Deve permitir upload quando status for rejeitado")
    void devePermitirUploadQuandoStatusForRejeitado() {
        var vendedor = criarVendedorPadrao();
        vendedor.setStatusVendedor(StatusVendedor.REJEITADO);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var arquivo = mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("contrato.pdf");
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(documentStorageService.salvarDocumento(arquivo)).thenReturn("http://localhost/doc.pdf");
        when(documentoRepository.save(any(DocumentoVendedor.class))).thenAnswer(i -> {
            DocumentoVendedor d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        var resultado = vendedorService.uploadDocumento(ID_PADRAO, TipoDocumento.CONTRATO_SOCIAL, arquivo);

        assertThat(resultado).isNotNull();
    }

    @Test
    @DisplayName("Deve lancar excecao quando status nao permitir upload")
    void deveLancarExcecaoQuandoStatusNaoPermitirUpload() {
        var vendedor = criarVendedorPadrao();
        vendedor.setStatusVendedor(StatusVendedor.APROVADO);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var arquivo = mock(MultipartFile.class);

        assertThatThrownBy(() -> vendedorService.uploadDocumento(ID_PADRAO, TipoDocumento.CNPJ_RECEITA, arquivo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode enviar documentos");
    }

    @Test
    @DisplayName("Deve avancar para pendente aprovacao quando todos documentos obrigatorios forem enviados")
    void deveAvancarParaPendenteAprovacaoQuandoTodosDocumentosObrigatoriosForemEnviados() {
        var vendedor = criarVendedorPadrao();
        vendedor.setStatusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var arquivo = mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("endereco.pdf");
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(documentStorageService.salvarDocumento(arquivo)).thenReturn("http://localhost/doc.pdf");
        when(documentoRepository.save(any(DocumentoVendedor.class))).thenAnswer(i -> {
            DocumentoVendedor d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.CNPJ_RECEITA)).thenReturn(true);
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.CONTRATO_SOCIAL)).thenReturn(true);
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.RG_RESPONSAVEL)).thenReturn(true);
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.COMPROVANTE_ENDERECO)).thenReturn(true);

        vendedorService.uploadDocumento(ID_PADRAO, TipoDocumento.COMPROVANTE_ENDERECO, arquivo);

        assertThat(vendedor.getStatusVendedor()).isEqualTo(StatusVendedor.PENDENTE_APROVACAO);
        verify(usuarioRepository).save(vendedor);
        verify(historicoRepository).save(any(HistoricoStatusVendedor.class));
    }

    @Test
    @DisplayName("Nao deve avancar status quando documentos obrigatorios estiverem incompletos")
    void naoDeveAvancarStatusQuandoDocumentosObrigatoriosEstiveremIncompletos() {
        var vendedor = criarVendedorPadrao();
        vendedor.setStatusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var arquivo = mock(MultipartFile.class);
        when(arquivo.getOriginalFilename()).thenReturn("cnpj.pdf");
        when(arquivo.getSize()).thenReturn(1024L);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(documentStorageService.salvarDocumento(arquivo)).thenReturn("http://localhost/doc.pdf");
        when(documentoRepository.save(any(DocumentoVendedor.class))).thenAnswer(i -> {
            DocumentoVendedor d = i.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.CNPJ_RECEITA)).thenReturn(true);
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.CONTRATO_SOCIAL)).thenReturn(false);
        when(documentoRepository.existsByUsuarioIdAndTipo(ID_PADRAO, TipoDocumento.MEI)).thenReturn(false);

        vendedorService.uploadDocumento(ID_PADRAO, TipoDocumento.CNPJ_RECEITA, arquivo);

        assertThat(vendedor.getStatusVendedor()).isEqualTo(StatusVendedor.PENDENTE_DOCUMENTOS);
    }

    // ===== buscarVendedorPorId =====

    @Test
    @DisplayName("Deve buscar vendedor por ID com sucesso")
    void deveBuscarVendedorPorIdComSucesso() {
        var vendedor = criarVendedorPadrao();
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(vendedor));

        var resultado = vendedorService.buscarVendedorPorId(ID_PADRAO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(ID_PADRAO);
    }

    @Test
    @DisplayName("Deve lancar excecao quando vendedor nao for encontrado")
    void deveLancarExcecaoQuandoVendedorNaoForEncontrado() {
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendedorService.buscarVendedorPorId(ID_PADRAO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    @DisplayName("Deve lancar excecao quando usuario nao for vendedor")
    void deveLancarExcecaoQuandoUsuarioNaoForVendedor() {
        var usuario = criarVendedorPadrao();
        usuario.setTipo(TipoUsuario.COMPRADOR);
        when(usuarioRepository.findById(ID_PADRAO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> vendedorService.buscarVendedorPorId(ID_PADRAO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não é um vendedor");
    }

    // ===== toResponse =====

    @Test
    @DisplayName("Deve converter usuario para response com aprovado por")
    void deveConverterUsuarioParaResponseComAprovadoPor() {
        var vendedor = criarVendedorPadrao();
        var admin = new Usuario();
        admin.setNome("Admin");
        vendedor.setAprovadoPor(admin);

        var resultado = vendedorService.toResponse(vendedor);

        assertThat(resultado.getNomeAprovadoPor()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("Deve converter usuario para response sem aprovado por")
    void deveConverterUsuarioParaResponseSemAprovadoPor() {
        var vendedor = criarVendedorPadrao();
        vendedor.setAprovadoPor(null);

        var resultado = vendedorService.toResponse(vendedor);

        assertThat(resultado.getNomeAprovadoPor()).isNull();
    }

    // ===== toDocResponse =====

    @Test
    @DisplayName("Deve converter documento para response com analisado por")
    void deveConverterDocumentoParaResponseComAnalisadoPor() {
        var doc = criarDocumento(TipoDocumento.CNPJ_RECEITA);
        var admin = new Usuario();
        admin.setNome("Admin Analista");
        doc.setAnalisadoPor(admin);

        var resultado = vendedorService.toDocResponse(doc);

        assertThat(resultado.getNomeAnalisadoPor()).isEqualTo("Admin Analista");
        assertThat(resultado.getTipoDescricao()).isEqualTo("Comprovante CNPJ");
    }

    @Test
    @DisplayName("Deve converter documento para response sem analisado por")
    void deveConverterDocumentoParaResponseSemAnalisadoPor() {
        var doc = criarDocumento(TipoDocumento.CONTRATO_SOCIAL);
        doc.setAnalisadoPor(null);

        var resultado = vendedorService.toDocResponse(doc);

        assertThat(resultado.getNomeAnalisadoPor()).isNull();
    }
}
