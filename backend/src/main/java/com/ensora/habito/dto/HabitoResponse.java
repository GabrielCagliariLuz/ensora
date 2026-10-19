package com.ensora.habito.dto;

import com.ensora.habito.Habito;
import com.ensora.habito.TipoHabito;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HabitoResponse(Long id, String nome, TipoHabito tipo, Integer vezesPorSemana) {

	public static HabitoResponse de(Habito habito, Integer vezesPorSemana) {
		return new HabitoResponse(habito.getId(), habito.getNome(), habito.getTipo(), vezesPorSemana);
	}

}
