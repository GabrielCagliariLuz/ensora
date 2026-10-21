package com.ensora.insights;

import com.ensora.compartilhado.data.Semana;
import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.TipoHabito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Cálculo do ritmo (seção 7.3): o quanto do que o usuário se propôs a fazer ele fez, dia a dia. Classe pura, sem
 * Spring nem banco, para que cada regra tenha o seu teste.
 *
 * <pre>
 * numerador   = registros do dia, de hábitos que existiam no dia
 * denominador = hábitos diários que existiam + soma de (meta da semana / 7) dos semanais que existiam
 * ritmo       = min(100%, numerador / denominador)
 * </pre>
 */
public final class RitmoCalculadora {

	private static final int DIAS_POR_BLOCO = 7;
	private static final int NUMERO_DE_BLOCOS = 4;

	private RitmoCalculadora() {
	}

	/** Ritmo em percentual (0 a 100, uma casa decimal). {@code ritmo} é nulo quando não há hábito existente. */
	public record DiaRitmo(LocalDate data, Double ritmo) {
	}

	public record BlocoRitmo(String rotulo, LocalDate de, LocalDate ate, Double ritmo) {
	}

	public record Resultado(List<DiaRitmo> dias, List<BlocoRitmo> semanas, Double ritmoMedio) {
	}

	/**
	 * Calcula o ritmo de cada dia de {@code de} a {@code ate} (inclusive). Os blocos S1 a S4 são de 7 dias a partir de
	 * {@code de}; o S4 vai até o fim do intervalo. Médias são aritméticas, sobre os dias que têm valor.
	 */
	public static Resultado calcular(List<HabitoHistorico> historico, LocalDate de, LocalDate ate) {
		int totalDeDias = (int) ChronoUnit.DAYS.between(de, ate) + 1;

		// Proporção de cada dia (0 a 1), sem arredondar, ou nula; as médias saem daqui, não dos valores arredondados.
		Double[] proporcoes = new Double[totalDeDias];
		List<DiaRitmo> dias = new ArrayList<>();
		for (int i = 0; i < totalDeDias; i++) {
			LocalDate dia = de.plusDays(i);
			proporcoes[i] = proporcaoDoDia(historico, dia);
			dias.add(new DiaRitmo(dia, emPercentual(proporcoes[i])));
		}

		List<BlocoRitmo> semanas = new ArrayList<>();
		for (int bloco = 0; bloco < NUMERO_DE_BLOCOS; bloco++) {
			int primeiro = bloco * DIAS_POR_BLOCO;
			if (primeiro >= totalDeDias) {
				break;
			}
			// O último bloco absorve o que sobra do intervalo (dias 29 a 31).
			int ultimo = bloco == NUMERO_DE_BLOCOS - 1 ? totalDeDias - 1 : Math.min(primeiro + DIAS_POR_BLOCO, totalDeDias) - 1;
			semanas.add(new BlocoRitmo("S" + (bloco + 1), de.plusDays(primeiro), de.plusDays(ultimo),
					emPercentual(media(proporcoes, primeiro, ultimo))));
		}

		return new Resultado(dias, semanas, emPercentual(media(proporcoes, 0, totalDeDias - 1)));
	}

	// Em "setimos" a conta é exata: cada diário vale 7 e cada semanal vale a meta da semana (meta/7 * 7).
	private static Double proporcaoDoDia(List<HabitoHistorico> historico, LocalDate dia) {
		LocalDate segunda = Semana.segundaFeiraDe(dia);
		int conclusoes = 0;
		int denominadorEmSetimos = 0;
		for (HabitoHistorico habito : historico) {
			if (!habito.existiaEm(dia)) {
				continue;
			}
			if (habito.tipo() == TipoHabito.DIARIO) {
				denominadorEmSetimos += 7;
			} else {
				Integer meta = habito.metaNaSemana(segunda);
				if (meta == null) {
					continue; // ainda sem meta nesta semana: o semanal não conta
				}
				denominadorEmSetimos += meta;
			}
			if (habito.diasFeitos().contains(dia)) {
				conclusoes++;
			}
		}
		if (denominadorEmSetimos == 0) {
			return null;
		}
		return Math.min(1.0, 7.0 * conclusoes / denominadorEmSetimos);
	}

	private static Double media(Double[] proporcoes, int primeiro, int ultimo) {
		double soma = 0;
		int dias = 0;
		for (int i = primeiro; i <= ultimo; i++) {
			if (proporcoes[i] != null) {
				soma += proporcoes[i];
				dias++;
			}
		}
		return dias == 0 ? null : soma / dias;
	}

	private static Double emPercentual(Double proporcao) {
		if (proporcao == null) {
			return null;
		}
		return BigDecimal.valueOf(proporcao * 100).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

}
