package com.leilao.vendor.repository;

import com.leilao.vendor.domain.StatusVendedor;
import com.leilao.vendor.domain.TipoUsuario;
import com.leilao.vendor.domain.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByCnpj(String cnpj);
    Optional<Usuario> findByEmailCorporativo(String emailCorporativo);

    boolean existsByEmail(String email);
    boolean existsByCnpj(String cnpj);

    List<Usuario> findByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario tipo, StatusVendedor status);
    List<Usuario> findByTipoAndAtivoTrue(TipoUsuario tipo);

    Page<Usuario> findByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario tipo, StatusVendedor status, Pageable pageable);
    Page<Usuario> findByTipoAndAtivoTrue(TipoUsuario tipo, Pageable pageable);

    long countByTipoAndAtivoTrue(TipoUsuario tipo);
    long countByTipoAndStatusVendedorAndAtivoTrue(TipoUsuario tipo, StatusVendedor status);
}
