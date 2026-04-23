package com.arremateai.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CadastroVendedorRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 200, message = "Nome deve ter no máximo 200 caracteres")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 200, message = "Email deve ter no máximo 200 caracteres")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*(),.?\":{}|<>]).{6,}$",
        message = "Senha deve conter pelo menos uma letra maiúscula e um caractere especial"
    )
    private String senha;

    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
    private String cpf;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 dígitos")
    private String telefone;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos")
    private String cnpj;

    @NotBlank(message = "Razão Social é obrigatória")
    @Size(max = 300, message = "Razão Social deve ter no máximo 300 caracteres")
    private String razaoSocial;

    @Size(max = 300, message = "Nome Fantasia deve ter no máximo 300 caracteres")
    private String nomeFantasia;

    @Size(max = 20, message = "Inscrição Estadual deve ter no máximo 20 caracteres")
    private String inscricaoEstadual;

    @NotBlank(message = "Email corporativo é obrigatório")
    @Email(message = "Email corporativo inválido")
    @Size(max = 200, message = "Email corporativo deve ter no máximo 200 caracteres")
    private String emailCorporativo;
}
