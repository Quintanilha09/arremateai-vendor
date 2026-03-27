package com.leilao.vendor.repository;

import com.leilao.vendor.domain.DocumentoVendedor;
import com.leilao.vendor.domain.StatusDocumento;
import com.leilao.vendor.domain.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentoVendedorRepository extends JpaRepository<DocumentoVendedor, UUID> {

    List<DocumentoVendedor> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndTipo(UUID usuarioId, TipoDocumento tipo);

    long countByUsuarioIdAndStatus(UUID usuarioId, StatusDocumento status);
}
