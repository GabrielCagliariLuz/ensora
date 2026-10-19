package com.ensora.habito.dto;

import com.ensora.habito.TipoHabito;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * {@code vezesPorSemana} e {@code data} só valem para hábito semanal: a meta é obrigatória nele e proibida no diário,
 * e a {@code data} (hoje, no fuso do usuário, enviada pelo front) define a segunda-feira da primeira vigência.
 */
public record HabitoRequest(
		@NotBlank @Size(max = 100) String nome,
		@NotNull TipoHabito tipo,
		@Min(1) @Max(7) Integer vezesPorSemana,
		LocalDate data) {
}
