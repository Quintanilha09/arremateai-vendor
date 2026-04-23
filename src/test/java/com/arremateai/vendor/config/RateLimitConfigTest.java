package com.arremateai.vendor.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitConfigTest {

    private RateLimitConfig rateLimitConfig;

    private static final String CHAVE_PADRAO = "usuario@empresa.com";
    private static final String OUTRA_CHAVE = "outro@empresa.com";
    private static final int LIMITE_MAXIMO = 5;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new RateLimitConfig();
    }

    @Test
    @DisplayName("Deve permitir primeira tentativa")
    void devePermitirPrimeiraTentativa() {
        var resultado = rateLimitConfig.tentativaPermitida(CHAVE_PADRAO);

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Deve permitir ate cinco tentativas para a mesma chave")
    void devePermitirAteCincoTentativasParaMesmaChave() {
        for (int i = 0; i < LIMITE_MAXIMO; i++) {
            assertThat(rateLimitConfig.tentativaPermitida(CHAVE_PADRAO))
                    .as("Tentativa %d deve ser permitida", i + 1)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Deve bloquear na sexta tentativa para a mesma chave")
    void deveBloquearNaSextaTentativaParaMesmaChave() {
        for (int i = 0; i < LIMITE_MAXIMO; i++) {
            rateLimitConfig.tentativaPermitida(CHAVE_PADRAO);
        }

        var resultado = rateLimitConfig.tentativaPermitida(CHAVE_PADRAO);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Deve manter buckets independentes para chaves diferentes")
    void deveMaterBucketsIndependentesParaChavesDiferentes() {
        for (int i = 0; i < LIMITE_MAXIMO; i++) {
            rateLimitConfig.tentativaPermitida(CHAVE_PADRAO);
        }

        var resultadoChaveEsgotada = rateLimitConfig.tentativaPermitida(CHAVE_PADRAO);
        var resultadoOutraChave = rateLimitConfig.tentativaPermitida(OUTRA_CHAVE);

        assertThat(resultadoChaveEsgotada).isFalse();
        assertThat(resultadoOutraChave).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar chave vazia sem lancar excecao")
    void deveAceitarChaveVaziaSemLancarExcecao() {
        var resultado = rateLimitConfig.tentativaPermitida("");

        assertThat(resultado).isTrue();
    }
}
