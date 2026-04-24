package com.arremateai.vendor.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("AdminVendedorController - Integração")
class AdminVendedorControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/admin/vendedores/pendentes sem role ADMIN retorna 403")
    void deveRejeitarSemRoleAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/vendedores/pendentes")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/vendedores/pendentes com role ADMIN retorna 200")
    void deveListarPendentesComAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/vendedores/pendentes")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/vendedores/{id} inexistente retorna 400")
    void deveRetornar400VendedorInexistente() throws Exception {
        mockMvc.perform(get("/api/admin/vendedores/" + UUID.randomUUID())
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/admin/vendedores/estatisticas com ADMIN retorna 200")
    void deveObterEstatisticas() throws Exception {
        mockMvc.perform(get("/api/admin/vendedores/estatisticas")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk());
    }
}
