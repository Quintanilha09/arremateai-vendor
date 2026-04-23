package com.arremateai.vendor.controller;

import com.arremateai.vendor.domain.StatusDocumento;
import com.arremateai.vendor.domain.StatusVendedor;
import com.arremateai.vendor.dto.*;
import com.arremateai.vendor.service.AdminVendedorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminVendedorControllerTest {

    @Mock
    private AdminVendedorService adminVendedorService;

    @InjectMocks
    private AdminVendedorController adminVendedorController;

    private static final String ADMIN_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID ADMIN_UUID = UUID.fromString(ADMIN_ID);
    private static final UUID VENDEDOR_ID = UUID.randomUUID();
    private static final UUID DOCUMENTO_ID = UUID.randomUUID();
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_COMPRADOR = "ROLE_COMPRADOR";

    private VendedorResponse criarVendedorResponse() {
        return VendedorResponse.builder()
                .id(VENDEDOR_ID)
                .nome("Vendedor Teste")
                .statusVendedor(StatusVendedor.PENDENTE_DOCUMENTOS)
                .build();
    }

    // ===== VERIFICAÇÃO DE ADMIN =====

    @Test
    @DisplayName("Deve retornar 403 ao listar pendentes sem role admin")
    void deveRetornar403AoListarPendentesSemRoleAdmin() {
        var resultado = adminVendedorController.listarPendentes(ADMIN_ID, ROLE_COMPRADOR, Pageable.ofSize(20));

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
        verify(adminVendedorService, never()).listarVendedoresPendentes(any());
    }

    // ===== listarPendentes =====

    @Test
    @DisplayName("Deve retornar página de vendedores pendentes")
    void deveRetornarPaginaDeVendedoresPendentes() {
        Page<VendedorResponse> pagina = new PageImpl<>(List.of(criarVendedorResponse()));
        when(adminVendedorService.listarVendedoresPendentes(any())).thenReturn(pagina);

        var resultado = adminVendedorController.listarPendentes(ADMIN_ID, ROLE_ADMIN, Pageable.ofSize(20));

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody().getContent()).hasSize(1);
    }

    // ===== listarTodos =====

    @Test
    @DisplayName("Deve retornar página de todos os vendedores")
    void deveRetornarPaginaDeTodosOsVendedores() {
        Page<VendedorResponse> pagina = new PageImpl<>(List.of(criarVendedorResponse()));
        when(adminVendedorService.listarTodosVendedores(any(), any())).thenReturn(pagina);

        var resultado = adminVendedorController.listarTodos(ADMIN_ID, ROLE_ADMIN, null, Pageable.ofSize(20));

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve retornar 403 ao listar todos sem role admin")
    void deveRetornar403AoListarTodosSemRoleAdmin() {
        var resultado = adminVendedorController.listarTodos(ADMIN_ID, ROLE_COMPRADOR, null, Pageable.ofSize(20));

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== buscarPorId =====

    @Test
    @DisplayName("Deve retornar vendedor por ID")
    void deveRetornarVendedorPorId() {
        when(adminVendedorService.buscarVendedorPorId(VENDEDOR_ID)).thenReturn(criarVendedorResponse());

        var resultado = adminVendedorController.buscarPorId(ADMIN_ID, ROLE_ADMIN, VENDEDOR_ID);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody().getId()).isEqualTo(VENDEDOR_ID);
    }

    @Test
    @DisplayName("Deve retornar 403 ao buscar vendedor sem role admin")
    void deveRetornar403AoBuscarVendedorSemRoleAdmin() {
        var resultado = adminVendedorController.buscarPorId(ADMIN_ID, ROLE_COMPRADOR, VENDEDOR_ID);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== aprovar =====

    @Test
    @DisplayName("Deve aprovar vendedor com sucesso")
    void deveAprovarVendedorComSucesso() {
        when(adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_UUID, "Bom cadastro"))
                .thenReturn(criarVendedorResponse());

        var request = new AprovarVendedorRequest();
        request.setComentario("Bom cadastro");
        var resultado = adminVendedorController.aprovar(VENDEDOR_ID, ADMIN_ID, ROLE_ADMIN, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve aprovar vendedor sem comentário")
    void deveAprovarVendedorSemComentario() {
        when(adminVendedorService.aprovarVendedor(VENDEDOR_ID, ADMIN_UUID, null))
                .thenReturn(criarVendedorResponse());

        var resultado = adminVendedorController.aprovar(VENDEDOR_ID, ADMIN_ID, ROLE_ADMIN, null);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        verify(adminVendedorService).aprovarVendedor(VENDEDOR_ID, ADMIN_UUID, null);
    }

    @Test
    @DisplayName("Deve retornar 403 ao aprovar sem role admin")
    void deveRetornar403AoAprovarSemRoleAdmin() {
        var resultado = adminVendedorController.aprovar(VENDEDOR_ID, ADMIN_ID, ROLE_COMPRADOR, null);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== rejeitar =====

    @Test
    @DisplayName("Deve rejeitar vendedor com motivo")
    void deveRejeitarVendedorComMotivo() {
        when(adminVendedorService.rejeitarVendedor(VENDEDOR_ID, ADMIN_UUID, "Documentação incompleta"))
                .thenReturn(criarVendedorResponse());

        var request = new RejeitarVendedorRequest();
        request.setMotivo("Documentação incompleta");
        var resultado = adminVendedorController.rejeitar(VENDEDOR_ID, ADMIN_ID, ROLE_ADMIN, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve retornar 403 ao rejeitar sem role admin")
    void deveRetornar403AoRejeitarSemRoleAdmin() {
        var request = new RejeitarVendedorRequest();
        request.setMotivo("Motivo");

        var resultado = adminVendedorController.rejeitar(VENDEDOR_ID, ADMIN_ID, ROLE_COMPRADOR, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== suspender =====

    @Test
    @DisplayName("Deve suspender vendedor com motivo")
    void deveSuspenderVendedorComMotivo() {
        when(adminVendedorService.suspenderVendedor(VENDEDOR_ID, ADMIN_UUID, "Violação de termos"))
                .thenReturn(criarVendedorResponse());

        var request = new RejeitarVendedorRequest();
        request.setMotivo("Violação de termos");
        var resultado = adminVendedorController.suspender(VENDEDOR_ID, ADMIN_ID, ROLE_ADMIN, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve retornar 403 ao suspender sem role admin")
    void deveRetornar403AoSuspenderSemRoleAdmin() {
        var request = new RejeitarVendedorRequest();
        request.setMotivo("Motivo");
        var resultado = adminVendedorController.suspender(VENDEDOR_ID, ADMIN_ID, ROLE_COMPRADOR, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== atualizarStatusDocumento =====

    @Test
    @DisplayName("Deve atualizar status de documento")
    void deveAtualizarStatusDeDocumento() {
        var request = AtualizarStatusDocumentoRequest.builder()
                .status(StatusDocumento.APROVADO)
                .build();
        var resposta = DocumentoVendedorResponse.builder().id(DOCUMENTO_ID).build();
        when(adminVendedorService.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_UUID, request)).thenReturn(resposta);

        var resultado = adminVendedorController.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, ROLE_ADMIN, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve retornar 403 ao atualizar documento sem role admin")
    void deveRetornar403AoAtualizarDocumentoSemRoleAdmin() {
        var request = AtualizarStatusDocumentoRequest.builder().status(StatusDocumento.APROVADO).build();

        var resultado = adminVendedorController.atualizarStatusDocumento(DOCUMENTO_ID, ADMIN_ID, ROLE_COMPRADOR, request);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }

    // ===== obterEstatisticas =====

    @Test
    @DisplayName("Deve retornar estatísticas de vendedores")
    void deveRetornarEstatisticasDeVendedores() {
        when(adminVendedorService.obterEstatisticas()).thenReturn(Map.of("total", 50L, "pendentes", 10L));

        var resultado = adminVendedorController.obterEstatisticas(ADMIN_ID, ROLE_ADMIN);

        assertThat(resultado.getStatusCode().value()).isEqualTo(200);
        assertThat(resultado.getBody()).containsEntry("total", 50L);
    }

    @Test
    @DisplayName("Deve retornar 403 ao buscar estatísticas sem role admin")
    void deveRetornar403AoBuscarEstatisticasSemRoleAdmin() {
        var resultado = adminVendedorController.obterEstatisticas(ADMIN_ID, ROLE_COMPRADOR);

        assertThat(resultado.getStatusCode().value()).isEqualTo(403);
    }
}
