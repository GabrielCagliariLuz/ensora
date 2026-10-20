package com.ensora.inicio.dto;

import com.ensora.habito.HabitoDoDia;
import com.ensora.habito.TipoHabito;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;

public record InicioResponse(LocalDate data, List<HabitoNaHome> habitos) {

	/** {@code feitos} e {@code meta} só aparecem no hábito semanal. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record HabitoNaHome(Long id, String nome, TipoHabito tipo, boolean feito, Integer feitos, Integer meta) {

		public static HabitoNaHome de(HabitoDoDia habito) {
			return new HabitoNaHome(habito.id(), habito.nome(), habito.tipo(), habito.feito(), habito.feitos(),
					habito.meta());
		}

	}

}
