package com.leilao.vendor.service;

import com.leilao.vendor.domain.*;
import com.leilao.vendor.dto.*;
import com.leilao.vendor.exception.BusinessException;
import com.leilao.vendor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminVendedorService {

    private final UsuarioRepository usuarioRepository;
    private final DocumentoVendedorRepository documentoRepository;
    private final HistoricoStatusVendedorRepository historicoRepository;
    private final AdminNotificationService adminNotificationService;
    private final VendedorService vendedorService;

    @Transactional(readOnly = true)
    public Page<VendedorResponse> listarVendedoresPendentes(Pageable pageable) {
        return usuarioRepository
                .findByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR,
                        StatusVendedor.PENDENTE_APROVACAO, pageable)
                .map(vendedorService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<VendedorResponse> listarTodosVendedores(StatusVendedor status, Pageable pageable) {
        if (status != null) {
            return usuarioRepository
                    .findByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario.VENDEDOR, status, pageable)
                    .map(vendedorService::toResponse);
        }
        return usuarioRepository.findByTipoAndAtivoTrue(TipoUsuario.VENDEDOR, pageable)
                .map(vendedorService::toResponse);
    }

    @Transactional(readOnly = true)
    public VendedorResponse buscarVendedorPorId(UUID id) {
        return vendedorService.toResponse(vendedorService.buscarVendedorPorId(id));
    }

    @Transactional
    public VendedorResponse aprovarVendedor(UUID vendedorId, UUID adminId, String comentario) {
        Usuario vendedor = vendedorService.buscarVendedorPorId(vendedorId);
        if (vendedor.getStatusVendedor() != StatusVendedor.PENDENTE_APROVACAO) {
            throw new IllegalStateException(
                    "Vendedor não está com status PENDENTE_APROVACAO. Status atual: "
                            + vendedor.getStatusVendedor());
        }
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado"));
        StatusVendedor anterior = vendedor.getStatusVendedor();
        vendedor.setStatusVendedor(StatusVendedor.APROVADO);
        vendedor.setAprovadoEm(LocalDateTime.now());
        vendedor.setAprovadoPor(admin);
        vendedor.setMotivoRejeicao(null);
        usuarioRepository.save(vendedor);
        registrarHistorico(vendedor, anterior, StatusVendedor.APROVADO,
                comentario != null ? comentario : "Aprovado pelo admin", admin);
        adminNotificationService.notificarVendedorAprovado(vendedor);
        log.info("Vendedor {} aprovado pelo admin {}", vendedorId, adminId);
        return vendedorService.toResponse(vendedor);
    }

    @Transactional
    public VendedorResponse rejeitarVendedor(UUID vendedorId, UUID adminId, String motivo) {
        Usuario vendedor = vendedorService.buscarVendedorPorId(vendedorId);
        if (vendedor.getStatusVendedor() != StatusVendedor.PENDENTE_APROVACAO
                && vendedor.getStatusVendedor() != StatusVendedor.PENDENTE_DOCUMENTOS) {
            throw new IllegalStateException(
                    "Vendedor não pode ser rejeitado neste momento. Status: "
                            + vendedor.getStatusVendedor());
        }
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado"));
        StatusVendedor anterior = vendedor.getStatusVendedor();
        vendedor.setStatusVendedor(StatusVendedor.REJEITADO);
        vendedor.setMotivoRejeicao(motivo);
        vendedor.setAprovadoEm(null);
        vendedor.setAprovadoPor(null);
        usuarioRepository.save(vendedor);
        registrarHistorico(vendedor, anterior, StatusVendedor.REJEITADO, motivo, admin);
        adminNotificationService.notificarVendedorRejeitado(vendedor, motivo);
        log.info("Vendedor {} rejeitado pelo admin {}", vendedorId, adminId);
        return vendedorService.toResponse(vendedor);
    }

    @Transactional
    public VendedorResponse suspenderVendedor(UUID vendedorId, UUID adminId, String motivo) {
        Usuario vendedor = vendedorService.buscarVendedorPorId(vendedorId);
        if (vendedor.getStatusVendedor() != StatusVendedor.APROVADO) {
            throw new IllegalStateException(
                    "Somente vendedores aprovados podem ser suspensos. Status: "
                            + vendedor.getStatusVendedor());
        }
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado"));
        StatusVendedor anterior = vendedor.getStatusVendedor();
        vendedor.setStatusVendedor(StatusVendedor.SUSPENSO);
        vendedor.setMotivoRejeicao(motivo);
        usuarioRepository.save(vendedor);
        registrarHistorico(vendedor, anterior, StatusVendedor.SUSPENSO, motivo, admin);
        log.info("Vendedor {} suspenso pelo admin {}", vendedorId, adminId);
        return vendedorService.toResponse(vendedor);
    }

    @Transactional
    public DocumentoVendedorResponse atualizarStatusDocumento(UUID documentoId, UUID adminId,
                                                               AtualizarStatusDocumentoRequest req) {
        DocumentoVendedor doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        if (doc.getStatus() != StatusDocumento.PENDENTE) {
            throw new IllegalStateException("Documento já foi analisado");
        }
        if (req.getStatus() == StatusDocumento.REJEITADO
                && (req.getMotivo() == null || req.getMotivo().isBlank())) {
            throw new BusinessException("Motivo é obrigatório ao rejeitar um documento");
        }
        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin não encontrado"));
        doc.setStatus(req.getStatus());
        doc.setMotivoRejeicao(req.getStatus() == StatusDocumento.REJEITADO ? req.getMotivo() : null);
        doc.setAnalisadoPor(admin);
        doc.setAnalisadoEm(LocalDateTime.now());
        documentoRepository.save(doc);
        return vendedorService.toDocResponse(doc);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> obterEstatisticas() {
        return Map.of(
                "pendentesDocumentos",
                usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(
                        TipoUsuario.VENDEDOR, StatusVendedor.PENDENTE_DOCUMENTOS),
                "pendentesAprovacao",
                usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(
                        TipoUsuario.VENDEDOR, StatusVendedor.PENDENTE_APROVACAO),
                "aprovados",
                usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(
                        TipoUsuario.VENDEDOR, StatusVendedor.APROVADO),
                "rejeitados",
                usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(
                        TipoUsuario.VENDEDOR, StatusVendedor.REJEITADO),
                "suspensos",
                usuarioRepository.countByTipoAndStatusVendedorAndAtivoTrue(
                        TipoUsuario.VENDEDOR, StatusVendedor.SUSPENSO)
        );
    }

    private void registrarHistorico(Usuario vendedor, StatusVendedor anterior,
                                    StatusVendedor novo, String motivo, Usuario alteradoPor) {
        HistoricoStatusVendedor h = HistoricoStatusVendedor.builder()
                .usuario(vendedor)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .motivo(motivo)
                .alteradoPor(alteradoPor)
                .build();
        historicoRepository.save(h);
    }
}
