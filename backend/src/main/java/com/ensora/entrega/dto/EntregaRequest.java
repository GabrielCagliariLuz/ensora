package com.ensora.entrega.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Corpo de criar e de editar. {@code prazoHorario} nulo quer dizer "sem horário": o app trata como 23:59.
 * A conclusão não entra aqui: só o PATCH a altera.
 */
public record EntregaRequest(
		@NotBlank @Size(max = 150) String titulo,
		@NotNull LocalDate prazoData,
		LocalTime prazoHorario,
		@Size(max = 1000) String nota) {
}
