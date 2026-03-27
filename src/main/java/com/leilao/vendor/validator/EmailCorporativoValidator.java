package com.leilao.vendor.validator;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EmailCorporativoValidator {

    private static final Set<String> DOMINIOS_PESSOAIS = Set.of(
            "gmail.com", "googlemail.com",
            "hotmail.com", "outlook.com", "live.com", "msn.com",
            "yahoo.com", "yahoo.com.br", "ymail.com",
            "icloud.com", "me.com", "mac.com",
            "aol.com", "protonmail.com", "proton.me", "zoho.com",
            "mail.com", "gmx.com", "gmx.net",
            "tempmail.com", "guerrillamail.com", "10minutemail.com",
            "mailinator.com", "maildrop.cc"
    );

    public boolean isEmailCorporativo(String email) {
        if (email == null || email.isBlank()) return false;
        int atIndex = email.indexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) return false;
        String dominio = email.substring(atIndex + 1).toLowerCase().trim();
        return !DOMINIOS_PESSOAIS.contains(dominio);
    }

    public void validarEmailCorporativo(String email) {
        if (!isEmailCorporativo(email)) {
            String dominio = email.contains("@") ? email.substring(email.indexOf('@') + 1) : email;
            throw new IllegalArgumentException(
                    "Email corporativo inválido. Use um email da sua empresa, não de provedores pessoais como " + dominio
            );
        }
    }
}
