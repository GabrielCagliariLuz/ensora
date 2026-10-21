package com.ensora.habito;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * O histórico de um hábito para os Insights, inclusive de hábitos já excluídos. {@code criadoDia} e
 * {@code excluidoDia} são dias no fuso do usuário. {@code metas} vem da vigência mais recente para a mais antiga e
 * {@code diasFeitos} traz só os dias com registro dentro do período pedido.
 */
public record HabitoHistorico(Long id, String nome, TipoHabito tipo, LocalDate criadoDia, LocalDate excluidoDia,
		List<MetaVigente> metas, Set<LocalDate> diasFeitos) {

	public record MetaVigente(LocalDate vigenteDesde, int vezesPorSemana) {
	}

	/** O hábito existe do dia da criação até o dia anterior ao da exclusão (o dia da exclusão não conta). */
	public boolean existiaEm(LocalDate dia) {
		return !dia.isBefore(criadoDia) && (excluidoDia == null || dia.isBefore(excluidoDia));
	}

	/**
	 * A meta que valia na semana que começa em {@code segunda}: a vigência de maior {@code vigente_desde} menor ou
	 * igual a ela. Nulo se o hábito ainda não tinha meta nessa semana (ou se é diário).
	 */
	public Integer metaNaSemana(LocalDate segunda) {
		for (MetaVigente meta : metas) {
			if (!meta.vigenteDesde().isAfter(segunda)) {
				return meta.vezesPorSemana();
			}
		}
		return null;
	}

}
