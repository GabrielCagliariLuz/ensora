package com.ensora.insights;

import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.HabitoService;
import com.ensora.insights.dto.RitmoResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Leitura agregada dos Insights: orquestra Services de outros módulos, nunca Repositories. */
@Service
public class InsightsService {

	/** Um mês: com mais que isso o S4 viraria um bloco gigante. */
	private static final long INTERVALO_MAXIMO_EM_DIAS = 31;

	private final HabitoService habitoService;

	public InsightsService(HabitoService habitoService) {
		this.habitoService = habitoService;
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
