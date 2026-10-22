package com.ensora.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Excluir a conta apaga tudo e não tem volta: a pessoa confirma com a senha. */
public record ExclusaoRequest(@NotBlank @Size(max = 72) String senha) {
}
