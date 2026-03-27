package com.leilao.vendor.repository;

import com.leilao.vendor.domain.CodigoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodigoVerificacaoRepository extends JpaRepository<CodigoVerificacao, UUID> {

    Optional<CodigoVerificacao> findByEmailAndCodigoAndVerificadoFalse(String email, String codigo);

    Optional<CodigoVerificacao> findFirstByEmailOrderByCreatedAtDesc(String email);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByEmail(String email);
}
