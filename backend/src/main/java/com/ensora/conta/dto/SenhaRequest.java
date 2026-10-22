package com.ensora.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SenhaRequest(
		@NotBlank @Size(max = 72) String senhaAtual,
		// Mesma regra do cadastro: de 8 a 72 caracteres (o teto é o limite do BCrypt).
		@NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
