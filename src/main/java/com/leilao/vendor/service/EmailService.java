package com.leilao.vendor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.2fa.from-email}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarCodigoVerificacao(String destinatario, String codigo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinatario);
            message.setSubject("ArremateAI - Código de Verificação");
            message.setText(String.format("""
                    Olá!

                    Seu código de verificação do ArremateAI é:

                    %s

                    Este código expira em 10 minutos.

                    Se você não solicitou este código, ignore este email.

                    Atenciosamente,
                    Equipe ArremateAI
                    """, codigo));
            mailSender.send(message);
            log.info("Email de verificação enviado para: {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email para {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Erro ao enviar email de verificação", e);
        }
    }

    public void enviarEmailSimples(String destinatario, String assunto, String corpo) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(destinatario);
            message.setSubject(assunto);
            message.setText(corpo);
            mailSender.send(message);
            log.info("Email enviado para: {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email para {}: {}", destinatario, e.getMessage());
        }
    }
}
