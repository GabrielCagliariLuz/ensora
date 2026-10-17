package com.ensora.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarRequest(
		@NotBlank @Email @Size(max = 255) String email,
		// O teto de 72 vem do BCrypt, que só lê os 72 primeiros bytes da senha.
		@NotBlank @Size(min = 4, max = 72) String senha) {
}
