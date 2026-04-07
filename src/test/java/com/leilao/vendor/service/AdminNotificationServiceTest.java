package com.leilao.vendor.service;

import com.leilao.vendor.domain.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    private static final String ADMIN_EMAIL = "admin@arremateai.com";
    private static final String FRONTEND_URL = "http://localhost:3000";

    private Usuario criarVendedorPadrao() {
        var vendedor = new Usuario();
        vendedor.setNome("Empresa Teste");
        vendedor.setEmail("vendedor@empresa.com");
        vendedor.setCnpj("12345678000199");
        return vendedor;
    }

    private void configurarValores() {
        ReflectionTestUtils.setField(adminNotificationService, "adminEmail", ADMIN_EMAIL);
        ReflectionTestUtils.setField(adminNotificationService, "frontendUrl", FRONTEND_URL);
    }

    @Test
    @DisplayName("Deve notificar admin sobre novo vendedor")
    void deveNotificarAdminSobreNovoVendedor() {
        configurarValores();
        var vendedor = criarVendedorPadrao();

        adminNotificationService.notificarNovoVendedor(vendedor);

        verify(emailService).enviarEmailSimples(
                eq(ADMIN_EMAIL),
                eq("Novo vendedor aguardando aprovação"),
                anyString()
        );
    }

    @Test
    @DisplayName("Deve notificar vendedor sobre aprovacao")
    void deveNotificarVendedorSobreAprovacao() {
        configurarValores();
        var vendedor = criarVendedorPadrao();

        adminNotificationService.notificarVendedorAprovado(vendedor);

        verify(emailService).enviarEmailSimples(
                eq("vendedor@empresa.com"),
                eq("Conta aprovada - ArremateAI"),
                anyString()
        );
    }

    @Test
    @DisplayName("Deve notificar vendedor sobre rejeicao com motivo")
    void deveNotificarVendedorSobreRejeicaoComMotivo() {
        configurarValores();
        var vendedor = criarVendedorPadrao();

        adminNotificationService.notificarVendedorRejeitado(vendedor, "Documentos incompletos");

        verify(emailService).enviarEmailSimples(
                eq("vendedor@empresa.com"),
                eq("Conta não aprovada - ArremateAI"),
                anyString()
        );
    }
}
