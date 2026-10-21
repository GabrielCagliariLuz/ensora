package com.ensora.insights.dto;

import com.ensora.insights.RitmoCalculadora.Resultado;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

/** Ritmo em percentual (0 a 100). {@code ritmo} e {@code ritmoMedio} só aparecem quando há hábito existente. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RitmoResponse(LocalDate de, LocalDate ate, Double ritmoMedio, List<Semana> semanas, List<Dia> dias) {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Semana(String rotulo, LocalDate de, LocalDate ate, Double ritmo) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Dia(LocalDate data, Double ritmo) {
	}

	public static RitmoResponse de(LocalDate de, LocalDate ate, Resultado resultado) {
		return new RitmoResponse(de, ate, resultado.ritmoMedio(),
				resultado.semanas().stream()
						.map(bloco -> new Semana(bloco.rotulo(), bloco.de(), bloco.ate(), bloco.ritmo()))
						.toList(),
				resultado.dias().stream().map(dia -> new Dia(dia.data(), dia.ritmo())).toList());
	}

}
