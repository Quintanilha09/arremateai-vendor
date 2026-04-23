package com.arremateai.vendor.service;

import com.arremateai.vendor.dto.CnpjResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CnpjValidationService {

    private static final String RECEITAWS_URL = "https://www.receitaws.com.br/v1/cnpj/";

    private final RestTemplate restTemplate;

    public CnpjValidationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CnpjResponseDTO validarCnpj(String cnpj) {
        try {
            String cnpjLimpo = cnpj.replaceAll("\\D", "");
            if (cnpjLimpo.length() != 14) {
                throw new IllegalArgumentException("CNPJ deve conter 14 dígitos");
            }
            CnpjResponseDTO response = restTemplate.getForObject(RECEITAWS_URL + cnpjLimpo, CnpjResponseDTO.class);
            if (response == null) {
                throw new IllegalArgumentException("Erro ao consultar CNPJ. Tente novamente mais tarde");
            }
            if (!response.isAtivo()) {
                throw new IllegalArgumentException(
                        "CNPJ com situação: " + response.getSituacao() + ". Apenas empresas ativas podem se cadastrar"
                );
            }
            log.info("CNPJ validado: {} - {}", response.getCnpj(), response.getRazaoSocial());
            return response;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar CNPJ: {}", e.getMessage());
            throw new IllegalArgumentException("Não foi possível validar o CNPJ. Verifique se está correto e tente novamente");
        }
    }
}
