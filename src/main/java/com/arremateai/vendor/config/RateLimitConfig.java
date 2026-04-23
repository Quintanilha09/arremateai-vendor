package com.arremateai.vendor.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private static final int CAPACIDADE_MAXIMA = 5;
    private static final Duration JANELA_TEMPO = Duration.ofMinutes(5);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tentativaPermitida(String chave) {
        Bucket bucket = buckets.computeIfAbsent(chave, this::criarBucket);
        return bucket.tryConsume(1);
    }

    private Bucket criarBucket(String chave) {
        Bandwidth limite = Bandwidth.classic(CAPACIDADE_MAXIMA,
                Refill.greedy(CAPACIDADE_MAXIMA, JANELA_TEMPO));
        return Bucket.builder().addLimit(limite).build();
    }
}
