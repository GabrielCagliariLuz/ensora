package com.ensora.insights.dto;

import com.ensora.habito.TipoHabito;
import com.ensora.insights.ConsistenciaCalculadora.Estado;
import com.ensora.insights.ConsistenciaCalculadora.HabitoConsistencia;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Grade hábito × dia e consistência de cada hábito no mês. Campos sem valor não aparecem no JSON. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConsistenciaResponse(String mes, List<Habito> habitos) {

	/** {@code consistencia} em percentual (0 a 100); {@code semanas} só existe no semanal. */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Habito(Long id, String nome, TipoHabito tipo, boolean excluido, Double consistencia,
			@JsonInclude(JsonInclude.Include.NON_EMPTY) List<Semana> semanas, List<Dia> dias) {
	}

	public record Semana(LocalDate de, LocalDate ate, int meta, int feitos, boolean batida, boolean encerrada,
			boolean contaNaConsistencia) {
	}

	public record Dia(LocalDate data, Estado estado) {
	}

	public static ConsistenciaResponse de(YearMonth mes, List<HabitoConsistencia> habitos) {
		return new ConsistenciaResponse(mes.toString(), habitos.stream().map(ConsistenciaResponse::de).toList());
	}

	private static Habito de(HabitoConsistencia h) {
		return new Habito(h.id(), h.nome(), h.tipo(), h.excluido(), h.consistencia(),
				h.semanas().stream()
						.map(s -> new Semana(s.de(), s.ate(), s.meta(), s.feitos(), s.batida(), s.encerrada(),
								s.contaNaConsistencia()))
						.toList(),
				h.dias().stream().map(c -> new Dia(c.data(), c.estado())).toList());
	}

}
