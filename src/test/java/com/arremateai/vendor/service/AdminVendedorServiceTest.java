package com.arremateai.vendor.service;

import com.arremateai.vendor.domain.*;
import com.arremateai.vendor.dto.AtualizarStatusDocumentoRequest;
import com.arremateai.vendor.dto.VendedorResponse;
import com.arremateai.vendor.exception.BusinessException;
import com.arremateai.vendor.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminVendedorServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private DocumentoVendedorRepository documentoRepository;

    @Mock
    private HistoricoStatusVendedorRepository historicoRepository;

    @Mock
    private AdminNotificationService adminNotificationService;

    @Mock
    private VendedorService vendedorService;

    @InjectMocks
    private AdminVendedorService adminVendedorService;

    private static final UUID VENDEDOR_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID DOCUMENTO_ID = UUID.randomUUID();
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private Usuario criarVendedorPadrao(StatusVendedor status) {
        var vendedor = new Usuario();
        vendedor.setId(VENDEDOR_ID);
        vendedor.setNome("Empresa Teste");
        vendedor.setEmail("vendedor@empresa.com");
        vendedor.setCnpj("12345678000199");
        vendedor.setTipo(TipoUsuario.VENDEDOR);
        vendedor.setStatusVendedor(status);
        vendedor.setAtivo(true);
        return vendedor;
    }

    private Usuario criarAdmin() {
        var admin = new Usuario();
        admin.setId(ADMIN_ID);
        admin.setNome("Admin Teste");
        admin.setEmail("admin@arremateai.com");
        admin.setTipo(TipoUsuario.ADMIN);
        return admin;
    }

    private VendedorResponse criarResponse() {
        return VendedorResponse.builder()
                .id(VENDEDOR_ID)
                .nome("Empresa Teste")
                .statusVendedor(StatusVendedor.PENDENTE_APROVACAO)
                .build();
    }

    private DocumentoVendedor criarDocumentoPendente() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        return DocumentoVendedor.builder()
                .id(DOCUMENTO_ID)
                .usuario(vendedor)
                .tipo(TipoDocumento.CNPJ_RECEITA)
                .nomeArquivo("cnpj.pdf")
                .url("http://localhost/doc.pdf")
                .tamanhoBytes(1024L)
                .mimeType("application/pdf")
                .status(StatusDocumento.PENDENTE)
                .build();
    }

    // ===== listarVendedoresPendentes =====

    @Test
    @DisplayName("Deve listar vendedores pendentes de aprovacao")
    void deveListarVendedoresPendentesDeAprovacao() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        Page<Usuario> pagina = new PageImpl<>(List.of(vendedor));
        when(usuarioRepository.findByTipoAndStatusVendedorAndAtivoTrue(
                TipoUsuario.VENDEDOR, StatusVendedor.PENDENTE_APROVACAO, PAGEABLE))
                .thenReturn(pagina);
        when(vendedorService.toResponse(vendedor)).thenReturn(criarResponse());

        var resultado = adminVendedorService.listarVendedoresPendentes(PAGEABLE);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    // ===== listarTodosVendedores =====

    @Test
    @DisplayName("Deve listar todos vendedores filtrados por status")
    void deveListarTodosVendedoresFiltradosPorStatus() {
        var vendedor = criarVendedorPadrao(StatusVendedor.APROVADO);
        Page<Usuario> pagina = new PageImpl<>(List.of(vendedor));
        when(usuarioRepository.findByTipoAndStatusVendedorAndAtivoTrue(
                TipoUsuario.VENDEDOR, StatusVendedor.APROVADO, PAGEABLE))
                .thenReturn(pagina);
        var response = VendedorResponse.builder().id(VENDEDOR_ID).statusVendedor(StatusVendedor.APROVADO).build();
        when(vendedorService.toResponse(vendedor)).thenReturn(response);

        var resultado = adminVendedorService.listarTodosVendedores(StatusVendedor.APROVADO, PAGEABLE);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve listar todos vendedores sem filtro de status")
    void deveListarTodosVendedoresSemFiltroDeStatus() {
        Page<Usuario> pagina = new PageImpl<>(List.of());
        when(usuarioRepository.findByTipoAndAtivoTrue(TipoUsuario.VENDEDOR, PAGEABLE))
                .thenReturn(pagina);

        var resultado = adminVendedorService.listarTodosVendedores(null, PAGEABLE);

        assertThat(resultado.getTotalElements()).isZero();
    }

    // ===== buscarVendedorPorId =====

    @Test
    @DisplayName("Deve buscar vendedor por ID via admin")
    void deveBuscarVendedorPorIdViaAdmin() {
        var vendedor = criarVendedorPadrao(StatusVendedor.APROVADO);
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(vendedorService.toResponse(vendedor)).thenReturn(criarResponse());

        var resultado = adminVendedorService.buscarVendedorPorId(VENDEDOR_ID);

        assertThat(resultado).isNotNull();
    }

    // ===== aprovarVendedor =====

    @Test
    @DisplayName("Deve aprovar vendedor com sucesso")
    void deveAprovarVendedorComSucesso() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        var admin = criarAdmin();
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        var response = VendedorResponse.builder().id(VENDEDOR_ID).statusVendedor(StatusVendedor.APROVADO).build();
        when(vendedorService.toResponse(vendedor)).thenReturn(response);

        var resultado = adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_ID, "Documentos OK");

        assertThat(resultado.getStatusVendedor()).isEqualTo(StatusVendedor.APROVADO);
        assertThat(vendedor.getAprovadoPor()).isEqualTo(admin);
        assertThat(vendedor.getMotivoRejeicao()).isNull();
        verify(adminNotificationService).notificarVendedorAprovado(vendedor);
        verify(historicoRepository).save(any(HistoricoStatusVendedor.class));
    }

    @Test
    @DisplayName("Deve aprovar vendedor com comentario nulo usando texto padrao")
    void deveAprovarVendedorComComentarioNuloUsandoTextoPadrao() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        var admin = criarAdmin();
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        when(vendedorService.toResponse(vendedor)).thenReturn(criarResponse());

        adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_ID, null);

        verify(historicoRepository).save(argThat(h ->
                h.getMotivo().equals("Aprovado pelo admin")
        ));
    }

    @Test
    @DisplayName("Deve lancar excecao ao aprovar vendedor que nao esta pendente de aprovacao")
    void deveLancarExcecaoAoAprovarVendedorQueNaoEstaPendenteDeAprovacao() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_DOCUMENTOS);
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);

        assertThatThrownBy(() -> adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_ID, "OK"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDENTE_APROVACAO");
    }

    @Test
    @DisplayName("Deve lancar excecao quando admin nao for encontrado ao aprovar")
    void deveLancarExcecaoQuandoAdminNaoForEncontradoAoAprovar() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_ID, "OK"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Admin não encontrado");
    }

    // ===== rejeitarVendedor =====

    @Test
    @DisplayName("Deve rejeitar vendedor pendente de aprovacao")
    void deveRejeitarVendedorPendenteDeAprovacao() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        var admin = criarAdmin();
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        var response = VendedorResponse.builder().id(VENDEDOR_ID).statusVendedor(StatusVendedor.REJEITADO).build();
        when(vendedorService.toResponse(vendedor)).thenReturn(response);

        var resultado = adminVendedorService.rejeitarVendedor(VENDEDOR_ID, ADMIN_ID, "Documentos inválidos");

        assertThat(resultado.getStatusVendedor()).isEqualTo(StatusVendedor.REJEITADO);
        assertThat(vendedor.getMotivoRejeicao()).isEqualTo("Documentos inválidos");
        verify(adminNotificationService).notificarVendedorRejeitado(vendedor, "Documentos inválidos");
    }

    @Test
    @DisplayName("Deve rejeitar vendedor pendente de documentos")
    void deveRejeitarVendedorPendenteDeDocumentos() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_DOCUMENTOS);
        var admin = criarAdmin();
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        when(vendedorService.toResponse(vendedor)).thenReturn(criarResponse());

        adminVendedorService.rejeitarVendedor(VENDEDOR_ID, ADMIN_ID, "Fraude detectada");

        assertThat(vendedor.getStatusVendedor()).isEqualTo(StatusVendedor.REJEITADO);
    }

    @Test
    @DisplayName("Deve lancar excecao ao rejeitar vendedor com status nao permitido")
    void deveLancarExcecaoAoRejeitarVendedorComStatusNaoPermitido() {
        var vendedor = criarVendedorPadrao(StatusVendedor.APROVADO);
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);

        assertThatThrownBy(() -> adminVendedorService.rejeitarVendedor(VENDEDOR_ID, ADMIN_ID, "Motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("não pode ser rejeitado");
    }

    // ===== suspenderVendedor =====

    @Test
    @DisplayName("Deve suspender vendedor aprovado com sucesso")
    void deveSuspenderVendedorAprovadoComSucesso() {
        var vendedor = criarVendedorPadrao(StatusVendedor.APROVADO);
        var admin = criarAdmin();
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(vendedor)).thenReturn(vendedor);
        var response = VendedorResponse.builder().id(VENDEDOR_ID).statusVendedor(StatusVendedor.SUSPENSO).build();
        when(vendedorService.toResponse(vendedor)).thenReturn(response);

        var resultado = adminVendedorService.suspenderVendedor(VENDEDOR_ID, ADMIN_ID, "Irregularidades");

        assertThat(resultado.getStatusVendedor()).isEqualTo(StatusVendedor.SUSPENSO);
        assertThat(vendedor.getMotivoRejeicao()).isEqualTo("Irregularidades");
        verify(historicoRepository).save(any(HistoricoStatusVendedor.class));
    }

    @Test
    @DisplayName("Deve lancar excecao ao suspender vendedor que nao esta aprovado")
    void deveLancarExcecaoAoSuspenderVendedorQueNaoEstaAprovado() {
        var vendedor = criarVendedorPadrao(StatusVendedor.PENDENTE_APROVACAO);
        when(vendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(vendedor);

        assertThatThrownBy(() -> adminVendedorService.suspenderVendedor(VENDEDOR_ID, ADMIN_ID, "Motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aprovados podem ser suspensos");
    }

    // ===== atualizarStatusDocumento =====

    @Test
    @DisplayName("Deve aprovar documento com sucesso")
    void deveAprovarDocumentoComSucesso() {
        var doc = criarDocumentoPendente();
        var admin = criarAdmin();
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.APROVADO)
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(doc));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(documentoRepository.save(doc)).thenReturn(doc);
        when(vendedorService.toDocResponse(doc)).thenReturn(
                com.arremateai.vendor.dto.DocumentoVendedorResponse.builder()
                        .id(DOCUMENTO_ID).status(StatusDocumento.APROVADO).build()
        );

        var resultado = adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request);

        assertThat(resultado.getStatus()).isEqualTo(StatusDocumento.APROVADO);
        assertThat(doc.getMotivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar documento com motivo")
    void deveRejeitarDocumentoComMotivo() {
        var doc = criarDocumentoPendente();
        var admin = criarAdmin();
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.REJEITADO)
                .motivo("Documento ilegível")
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(doc));
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(documentoRepository.save(doc)).thenReturn(doc);
        when(vendedorService.toDocResponse(doc)).thenReturn(
                com.arremateai.vendor.dto.DocumentoVendedorResponse.builder()
                        .id(DOCUMENTO_ID).status(StatusDocumento.REJEITADO).build()
        );

        var resultado = adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request);

        assertThat(resultado.getStatus()).isEqualTo(StatusDocumento.REJEITADO);
        assertThat(doc.getMotivoRejeicao()).isEqualTo("Documento ilegível");
    }

    @Test
    @DisplayName("Deve lancar excecao ao rejeitar documento sem motivo")
    void deveLancarExcecaoAoRejeitarDocumentoSemMotivo() {
        var doc = criarDocumentoPendente();
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.REJEITADO)
                .motivo(null)
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Motivo é obrigatório");
    }

    @Test
    @DisplayName("Deve lancar excecao ao rejeitar documento com motivo vazio")
    void deveLancarExcecaoAoRejeitarDocumentoComMotivoVazio() {
        var doc = criarDocumentoPendente();
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.REJEITADO)
                .motivo("   ")
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Motivo é obrigatório");
    }

    @Test
    @DisplayName("Deve lancar excecao quando documento ja foi analisado")
    void deveLancarExcecaoQuandoDocumentoJaFoiAnalisado() {
        var doc = criarDocumentoPendente();
        doc.setStatus(StatusDocumento.APROVADO);
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.REJEITADO)
                .motivo("Motivo")
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já foi analisado");
    }

    @Test
    @DisplayName("Deve lancar excecao quando documento nao for encontrado")
    void deveLancarExcecaoQuandoDocumentoNaoForEncontrado() {
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.APROVADO)
                .build();
        when(documentoRepository.findById(DOCUMENTO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Documento não encontrado");
    }

    // ===== obterEstatisticas =====

    @Test
    @DisplayName("Deve retornar estatisticas de vendedores")
    void deveRetornarEstatisticasDeVendedores() {
        when(usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, StatusVendedor.PENDENTE_DOCUMENTOS)).thenReturn(5L);
        when(usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, StatusVendedor.PENDENTE_APROVACAO)).thenReturn(3L);
        when(usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, StatusVendedor.APROVADO)).thenReturn(10L);
        when(usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, StatusVendedor.REJEITADO)).thenReturn(2L);
        when(usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, StatusVendedor.SUSPENSO)).thenReturn(1L);

        var resultado = adminVendedorService.obterEstatisticas();

        assertThat(resultado).containsEntry("pendentesDocumentos", 5L);
        assertThat(resultado).containsEntry("pendentesAprovacao", 3L);
        assertThat(resultado).containsEntry("aprovados", 10L);
        assertThat(resultado).containsEntry("rejeitados", 2L);
        assertThat(resultado).containsEntry("suspensos", 1L);
    }
}
