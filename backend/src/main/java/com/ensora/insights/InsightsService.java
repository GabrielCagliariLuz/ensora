package com.ensora.insights;

import com.ensora.compartilhado.data.Semana;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.HabitoService;
import com.ensora.insights.dto.ConsistenciaResponse;
import com.ensora.insights.dto.RitmoResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Leitura agregada dos Insights: orquestra Services de outros módulos, nunca Repositories. */
@Service
public class InsightsService {

	/** Um mês: com mais que isso o S4 viraria um bloco gigante. */
	private static final long INTERVALO_MAXIMO_EM_DIAS = 31;

	private final HabitoService habitoService;
	private final Clock relogio;
	private final ZoneId fuso;

	public InsightsService(HabitoService habitoService, Clock relogio, ZoneId fuso) {
		this.habitoService = habitoService;
		this.relogio = relogio;
		this.fuso = fuso;
	}

	/**
	 * Grade hábito × dia e consistência do mês. O "hoje" é do servidor, no fuso fixo da decisão 56. O histórico vem de
	 * semanas inteiras (da segunda do primeiro dia ao domingo do último), porque a meta do semanal se confere na
	 * semana toda, mesmo quando ela atravessa o mês.
	 */
	public ConsistenciaResponse consistencia(Long usuarioId, YearMonth mes) {
		LocalDate de = Semana.segundaFeiraDe(mes.atDay(1));
		LocalDate ate = Semana.segundaFeiraDe(mes.atEndOfMonth()).plusDays(6);
		var historico = habitoService.historicoParaInsights(usuarioId, de, ate);
		LocalDate hoje = LocalDate.now(relogio.withZone(fuso));
		return ConsistenciaResponse.de(mes, ConsistenciaCalculadora.calcular(historico, mes, hoje));
	}

	/**
	 * O front envia {@code ate} = hoje quando o mês ainda não terminou: todo dia do intervalo conta como já decorrido,
	 * e o servidor não precisa decidir o que é "hoje".
	 */
	public RitmoResponse ritmo(Long usuarioId, LocalDate de, LocalDate ate) {
		validarIntervalo(de, ate);
		var historico = habitoService.historicoParaInsights(usuarioId, de, ate);
		return RitmoResponse.de(de, ate, RitmoCalculadora.calcular(historico, de, ate));
	}

	private void validarIntervalo(LocalDate de, LocalDate ate) {
		if (ate.isBefore(de)) {
			throw new ValidacaoException("ate", "não pode ser antes de 'de'");
		}
		if (ChronoUnit.DAYS.between(de, ate) + 1 > INTERVALO_MAXIMO_EM_DIAS) {
			throw new ValidacaoException("ate", "intervalo maior que " + INTERVALO_MAXIMO_EM_DIAS + " dias");
		}
	}

}
