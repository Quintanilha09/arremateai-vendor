package com.leilao.vendor.repository;

import com.leilao.vendor.domain.HistoricoStatusVendedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HistoricoStatusVendedorRepository extends JpaRepository<HistoricoStatusVendedor, UUID> {
}
