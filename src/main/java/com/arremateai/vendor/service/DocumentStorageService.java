package com.arremateai.vendor.service;

import com.arremateai.vendor.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentStorageService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Value("${app.vendor.storage-location:./uploads/documentos}")
    private String storagePath;

    @Value("${app.vendor.base-url:http://localhost:8084}")
    private String baseUrl;

    private Path storageRoot;

    @PostConstruct
    public void init() throws IOException {
        storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(storageRoot);
        log.info("Document storage initialized successfully");
    }

    public String salvarDocumento(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessException("Arquivo não pode estar vazio");
        }
        if (arquivo.getSize() > MAX_SIZE) {
            throw new BusinessException("Arquivo excede o tamanho máximo de 10MB");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !Set.of("application/pdf", "image/jpeg", "image/png").contains(contentType)) {
            throw new BusinessException("Tipo de arquivo não permitido. Envie PDF, JPG ou PNG");
        }
        String ext = extrairExtensao(arquivo.getOriginalFilename());
        if (!EXTENSOES_PERMITIDAS.contains(ext.toLowerCase())) {
            throw new BusinessException("Extensão não permitida: " + ext);
        }
        String filename = UUID.randomUUID() + "." + ext;
        Path destino = storageRoot.resolve(filename);
        try {
            Files.copy(arquivo.getInputStream(), destino);
            log.debug("Documento salvo: {}", filename);
            return baseUrl + "/api/vendedores/documentos/arquivo/" + filename;
        } catch (IOException e) {
            log.error("Erro ao salvar documento: {}", e.getMessage(), e);
            throw new BusinessException("Erro ao salvar arquivo");
        }
    }

    private String extrairExtensao(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
