package com.leilao.vendor.service;

import com.leilao.vendor.domain.CodigoVerificacao;
import com.leilao.vendor.repository.CodigoVerificacaoRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@Slf4j
public class VerificacaoService {

    private static final SecureRandom random = new SecureRandom();

    private final CodigoVerificacaoRepository codigoRepository;
    private final EmailService emailService;

    @Value("${app.2fa.code-expiration-minutes}")
    private int expirationMinutes;

    public VerificacaoService(CodigoVerificacaoRepository codigoRepository, EmailService emailService) {
        this.codigoRepository = codigoRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void enviarCodigoVerificacao(String email, String dadosCadastroJson) {
        String emailNorm = email.trim().toLowerCase();
        codigoRepository.deleteByEmail(emailNorm);
        String codigo = String.valueOf(100000 + random.nextInt(900000));
        CodigoVerificacao cv = new CodigoVerificacao();
        cv.setEmail(emailNorm);
        cv.setCodigo(codigo);
        cv.setExpiresAt(LocalDateTime.now().plusMinutes(expirationMinutes));
        cv.setVerificado(false);
        cv.setDadosCadastro(dadosCadastroJson);
        codigoRepository.save(cv);
        emailService.enviarCodigoVerificacao(email, codigo);
        log.info("Código de verificação enviado para: {}", email);
    }

    @Transactional
    public CodigoVerificacao verificarCodigo(String email, String codigo) {
        String emailNorm = email.trim().toLowerCase();
        var opt = codigoRepository.findByEmailAndCodigoAndVerificadoFalse(emailNorm, codigo.trim());
        if (opt.isEmpty()) {
            log.warn("Código inválido ou já usado para: {}", emailNorm);
            return null;
        }
        CodigoVerificacao cv = opt.get();
        if (cv.isExpired()) {
            log.warn("Código expirado para: {}", emailNorm);
            return null;
        }
        cv.setVerificado(true);
        cv.setVerifiedAt(LocalDateTime.now());
        codigoRepository.save(cv);
        log.info("Código verificado com sucesso para: {}", emailNorm);
        return cv;
    }
}
