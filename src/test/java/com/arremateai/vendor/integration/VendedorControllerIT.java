package com.arremateai.vendor.integration;

import com.arremateai.vendor.dto.CadastroVendedorRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("VendedorController - Integração")
class VendedorControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/vendedores/registrar com payload vazio retorna 400")
    void deveRejeitarRegistroComPayloadInvalido() throws Exception {
        mockMvc.perform(post("/api/vendedores/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/vendedores/registrar com CNPJ inválido retorna 400")
    void deveRejeitarRegistroComCnpjInvalido() throws Exception {
        CadastroVendedorRequest req = CadastroVendedorRequest.builder()
                .nome("Teste")
                .email("teste@example.com")
                .senha("Senha@123")
                .cnpj("123")
                .razaoSocial("Razão Teste")
                .emailCorporativo("corp@example.com")
                .build();
        mockMvc.perform(post("/api/vendedores/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/vendedores/meu-status com usuário inexistente retorna 400")
    void deveRetornar400StatusInexistente() throws Exception {
        mockMvc.perform(get("/api/vendedores/meu-status")
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/vendedores/documentos/arquivo/{file} sem X-User-Id retorna 401")
    void deveRejeitarAcessoArquivoSemUsuario() throws Exception {
        mockMvc.perform(get("/api/vendedores/documentos/arquivo/qualquer.pdf"))
                .andExpect(status().isUnauthorized());
    }
}
