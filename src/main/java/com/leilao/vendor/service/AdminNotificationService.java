package com.leilao.vendor.service;

import com.leilao.vendor.domain.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdminNotificationService {

    private final EmailService emailService;

    @Value("${app.admin.email:admin@arremateai.com}")
    private String adminEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public AdminNotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void notificarNovoVendedor(Usuario vendedor) {
        String corpo = String.format("""
                Olá Administrador,

                Um novo vendedor se cadastrou e está aguardando aprovação:

                Nome: %s
                CNPJ: %s
                E-mail: %s

                Acesse: %s/admin/vendedores

                ---
                ArremateAI
                """, vendedor.getNome(), vendedor.getCnpj(), vendedor.getEmail(), frontendUrl);
        emailService.enviarEmailSimples(adminEmail, "Novo vendedor aguardando aprovação", corpo);
    }

    public void notificarVendedorAprovado(Usuario vendedor) {
        String corpo = String.format("""
                Olá %s,

                Sua conta de vendedor foi aprovada! Acesse: %s

                ---
                Equipe ArremateAI
                """, vendedor.getNome(), frontendUrl);
        emailService.enviarEmailSimples(vendedor.getEmail(), "Conta aprovada - ArremateAI", corpo);
    }

    public void notificarVendedorRejeitado(Usuario vendedor, String motivo) {
        String corpo = String.format("""
                Olá %s,

                Sua solicitação de cadastro não foi aprovada.

                Motivo: %s

                Dúvidas? Entre em contato: suporte@arremateai.com

                ---
                Equipe ArremateAI
                """, vendedor.getNome(), motivo);
        emailService.enviarEmailSimples(vendedor.getEmail(), "Conta não aprovada - ArremateAI", corpo);
    }
}
