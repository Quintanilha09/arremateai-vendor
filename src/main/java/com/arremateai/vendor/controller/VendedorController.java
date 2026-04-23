package com.arremateai.vendor.controller;

import com.arremateai.vendor.config.RateLimitConfig;
import com.arremateai.vendor.domain.TipoDocumento;
import com.arremateai.vendor.dto.CadastroVendedorRequest;
import com.arremateai.vendor.dto.DocumentoVendedorResponse;
import com.arremateai.vendor.dto.VendedorResponse;
import com.arremateai.vendor.service.VendedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendedores")
@RequiredArgsConstructor
public class VendedorController {

    private final VendedorService vendedorService;
    private final RateLimitConfig rateLimitConfig;

    @org.springframework.beans.factory.annotation.Value("${app.vendor.storage-location:./uploads/documentos}")
    private String storagePath;

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, String>> registrar(
            @Valid @RequestBody CadastroVendedorRequest request) {
        return ResponseEntity.ok(vendedorService.registrarVendedor(request));
    }

    @PostMapping("/verificar-email-corporativo")
    public ResponseEntity<VendedorResponse> verificarEmailCorporativo(
            @RequestParam String emailCorporativo,
            @RequestParam String codigo) {
        if (!rateLimitConfig.tentativaPermitida("verificar:" + emailCorporativo)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        return ResponseEntity.ok(
                vendedorService.verificarEmailCorporativo(emailCorporativo, codigo));
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Map<String, String>> reenviarCodigo(
            @RequestParam String emailCorporativo) {
        if (!rateLimitConfig.tentativaPermitida("reenviar:" + emailCorporativo)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        vendedorService.reenviarCodigoVerificacao(emailCorporativo);
        return ResponseEntity.ok(Map.of("message", "Código reenviado com sucesso."));
    }

    @GetMapping("/meu-status")
    public ResponseEntity<VendedorResponse> buscarMeuStatus(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(vendedorService.buscarMeuStatus(UUID.fromString(userId)));
    }

    @GetMapping("/meus-documentos")
    public ResponseEntity<List<DocumentoVendedorResponse>> listarMeusDocumentos(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(vendedorService.listarMeusDocumentos(UUID.fromString(userId)));
    }

    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoVendedorResponse> uploadDocumento(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam("tipo") TipoDocumento tipo,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(
                vendedorService.uploadDocumento(UUID.fromString(userId), tipo, arquivo));
    }

    @GetMapping("/documentos/arquivo/{filename:.+}")
    public ResponseEntity<Resource> servirDocumento(
            @PathVariable String filename,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(filename).toAbsolutePath().normalize();
        if (!filePath.startsWith(basePath)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            UUID userUuid;
            try {
                userUuid = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!vendedorService.verificarAcessoArquivo(filename, userUuid)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        FileSystemResource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (Exception e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + safeFilename + "\"")
                .body(resource);
    }
}
