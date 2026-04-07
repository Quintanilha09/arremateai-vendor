package com.leilao.vendor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private static final String FROM_EMAIL = "noreply@arremateai.com";
    private static final String DESTINATARIO = "usuario@empresa.com";
    private static final String CODIGO = "123456";

    private void configurarFromEmail() {
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
    }

    // ===== enviarCodigoVerificacao =====

    @Test
    @DisplayName("Deve enviar email de verificacao com sucesso")
    void deveEnviarEmailDeVerificacaoComSucesso() {
        configurarFromEmail();

        emailService.enviarCodigoVerificacao(DESTINATARIO, CODIGO);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando falhar ao enviar codigo de verificacao")
    void deveLancarExcecaoQuandoFalharAoEnviarCodigoDeVerificacao() {
        configurarFromEmail();
        doThrow(new RuntimeException("Erro SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.enviarCodigoVerificacao(DESTINATARIO, CODIGO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erro ao enviar email de verificação");
    }

    // ===== enviarEmailSimples =====

    @Test
    @DisplayName("Deve enviar email simples com sucesso")
    void deveEnviarEmailSimplesComSucesso() {
        configurarFromEmail();

        emailService.enviarEmailSimples(DESTINATARIO, "Assunto", "Corpo do email");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Nao deve lancar excecao quando falhar ao enviar email simples")
    void naoDeveLancarExcecaoQuandoFalharAoEnviarEmailSimples() {
        configurarFromEmail();
        doThrow(new RuntimeException("Erro SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.enviarEmailSimples(DESTINATARIO, "Assunto", "Corpo");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
