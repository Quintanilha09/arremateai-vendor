package com.arremateai.vendor.controller;

import com.arremateai.vendor.domain.StatusVendedor;
import com.arremateai.vendor.dto.*;
import com.arremateai.vendor.service.AdminVendedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/vendedores")
@RequiredArgsConstructor
public class AdminVendedorController {

    private final AdminVendedorService adminVendedorService;

    private ResponseEntity<?> verificarAdmin(String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        return null;
    }

    @GetMapping("/pendentes")
    public ResponseEntity<Page<VendedorResponse>> listarPendentes(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @PageableDefault(size = 20) Pageable pageable) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<Page<VendedorResponse>>) check;
        return ResponseEntity.ok(adminVendedorService.listarVendedoresPendentes(pageable));
    }

    @GetMapping
    public ResponseEntity<Page<VendedorResponse>> listarTodos(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(required = false) StatusVendedor status,
            @PageableDefault(size = 20) Pageable pageable) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<Page<VendedorResponse>>) check;
        return ResponseEntity.ok(adminVendedorService.listarTodosVendedores(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendedorResponse> buscarPorId(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable UUID id) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<VendedorResponse>) check;
        return ResponseEntity.ok(adminVendedorService.buscarVendedorPorId(id));
    }

    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<VendedorResponse> aprovar(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody(required = false) AprovarVendedorRequest request) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<VendedorResponse>) check;
        String comentario = request != null ? request.getComentario() : null;
        return ResponseEntity.ok(
                adminVendedorService.aprovarVendedor(id, UUID.fromString(adminId), comentario));
    }

    @PatchMapping("/{id}/rejeitar")
    public ResponseEntity<VendedorResponse> rejeitar(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody RejeitarVendedorRequest request) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<VendedorResponse>) check;
        return ResponseEntity.ok(
                adminVendedorService.rejeitarVendedor(id, UUID.fromString(adminId),
                        request.getMotivo()));
    }

    @PatchMapping("/{id}/suspender")
    public ResponseEntity<VendedorResponse> suspender(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody RejeitarVendedorRequest request) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<VendedorResponse>) check;
        return ResponseEntity.ok(
                adminVendedorService.suspenderVendedor(id, UUID.fromString(adminId),
                        request.getMotivo()));
    }

    @PatchMapping("/documentos/{documentoId}/status")
    public ResponseEntity<DocumentoVendedorResponse> atualizarStatusDocumento(
            @PathVariable UUID documentoId,
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody AtualizarStatusDocumentoRequest request) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<DocumentoVendedorResponse>) check;
        return ResponseEntity.ok(
                adminVendedorService.atualizarStatusDocumento(documentoId,
                        UUID.fromString(adminId), request));
    }

    @GetMapping("/estatisticas")
    public ResponseEntity<Map<String, Long>> obterEstatisticas(
            @RequestHeader("X-User-Id") String adminId,
            @RequestHeader("X-User-Role") String role) {
        ResponseEntity<?> check = verificarAdmin(role);
        if (check != null) return (ResponseEntity<Map<String, Long>>) check;
        return ResponseEntity.ok(adminVendedorService.obterEstatisticas());
    }
}
