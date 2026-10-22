package com.ensora.insights;

import com.ensora.compartilhado.data.Semana;
import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.TipoHabito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Consistência de cada hábito no mês e o estado de cada célula da grade hábito × dia (seções 7.1, 7.2 e 7.4). Classe
 * pura, sem Spring nem banco. O {@code historico} precisa trazer os registros de semanas inteiras (da segunda do
 * primeiro dia do mês ao domingo do último), porque a meta do semanal se confere na semana toda.
 */
public final class ConsistenciaCalculadora {

	private ConsistenciaCalculadora() {
	}

	public enum Estado {
		CONCLUIDO, DESCANSO_FUTURO, NAO_REALIZADO, EM_ABERTO
	}

	public record Celula(LocalDate data, Estado estado) {
	}

	/** Uma semana (segunda a domingo) que toca o mês, com o resumo {@code feitos/meta}. */
	public record SemanaDoHabito(LocalDate de, LocalDate ate, int meta, int feitos, boolean batida, boolean encerrada,
			boolean contaNaConsistencia) {
	}

	/** {@code consistencia} é um percentual (0 a 100, uma casa) ou nulo se não há o que considerar. */
	public record HabitoConsistencia(Long id, String nome, TipoHabito tipo, boolean excluido, Double consistencia,
			List<SemanaDoHabito> semanas, List<Celula> dias) {
	}

	/** Devolve só os hábitos com ao menos uma célula no mês, na ordem recebida. */
	public static List<HabitoConsistencia> calcular(List<HabitoHistorico> historico, YearMonth mes, LocalDate hoje) {
		List<HabitoConsistencia> resultado = new ArrayList<>();
		for (HabitoHistorico habito : historico) {
			HabitoConsistencia consistencia = habito.tipo() == TipoHabito.DIARIO
					? diario(habito, mes, hoje)
					: semanal(habito, mes, hoje);
			if (!consistencia.dias().isEmpty()) {
				resultado.add(consistencia);
			}
		}
		return resultado;
	}

	private static HabitoConsistencia diario(HabitoHistorico habito, YearMonth mes, LocalDate hoje) {
		LocalDate primeiro = mes.atDay(1);
		LocalDate ultimo = mes.atEndOfMonth();

		List<Celula> dias = new ArrayList<>();
		for (LocalDate dia = primeiro; !dia.isAfter(ultimo); dia = dia.plusDays(1)) {
			if (!habito.existiaEm(dia)) {
				continue;
			}
			Estado estado;
			if (habito.diasFeitos().contains(dia)) {
				estado = Estado.CONCLUIDO;
			} else if (dia.isAfter(hoje)) {
				estado = Estado.DESCANSO_FUTURO;
			} else if (dia.equals(hoje)) {
				estado = Estado.EM_ABERTO;
			} else {
				estado = Estado.NAO_REALIZADO;
			}
			dias.add(new Celula(dia, estado));
		}

		// Dias considerados: da criação (ou do início do mês) até o último dia do mês, hoje ou o dia anterior à exclusão.
		LocalDate inicio = maior(primeiro, habito.criadoDia());
		LocalDate fim = menor(ultimo, hoje);
		if (habito.excluidoDia() != null) {
			fim = menor(fim, habito.excluidoDia().minusDays(1));
		}
		int considerados = 0;
		int feitos = 0;
		for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
			considerados++;
			if (habito.diasFeitos().contains(dia)) {
				feitos++;
			}
		}
		Double consistencia = considerados == 0 ? null : emPercentual((double) feitos / considerados);

		return new HabitoConsistencia(habito.id(), habito.nome(), habito.tipo(), habito.excluidoDia() != null,
				consistencia, List.of(), dias);
	}

	private static HabitoConsistencia semanal(HabitoHistorico habito, YearMonth mes, LocalDate hoje) {
		LocalDate primeiro = mes.atDay(1);
		LocalDate ultimo = mes.atEndOfMonth();

		List<SemanaDoHabito> semanas = new ArrayList<>();
		List<Celula> dias = new ArrayList<>();
		// Dias esperados (soma das metas) e dias feitos (os de cada semana, limitados à meta) das semanas que contam.
		int esperados = 0;
		int feitosContados = 0;

		for (LocalDate segunda = Semana.segundaFeiraDe(primeiro); !segunda.isAfter(ultimo); segunda = segunda.plusDays(7)) {
			Integer meta = habito.metaNaSemana(segunda);
			if (meta == null) {
				continue; // ainda sem meta nesta semana: o semanal não existia
			}
			LocalDate domingo = segunda.plusDays(6);

			int feitos = 0;
			for (LocalDate dia = segunda; !dia.isAfter(domingo); dia = dia.plusDays(1)) {
				if (habito.existiaEm(dia) && habito.diasFeitos().contains(dia)) {
					feitos++;
				}
			}
			boolean batida = feitos >= meta;
			boolean encerrada = domingo.isBefore(hoje);

			boolean temCelulaNoMes = false;
			for (LocalDate dia = segunda; !dia.isAfter(domingo); dia = dia.plusDays(1)) {
				if (dia.isBefore(primeiro) || dia.isAfter(ultimo) || !habito.existiaEm(dia)) {
					continue;
				}
				temCelulaNoMes = true;
				dias.add(new Celula(dia, estadoDoSemanal(habito, dia, hoje, batida, encerrada)));
			}
			if (!temCelulaNoMes) {
				continue;
			}

			// A semana pertence ao mês da sua segunda-feira; a em andamento só conta se já bateu a meta.
			boolean daEsteMes = !segunda.isBefore(primeiro);
			boolean conta = daEsteMes && (encerrada || batida);
			if (conta) {
				esperados += meta;
				// Dia além da meta não compensa outra semana: cada semana vale, no máximo, a sua meta.
				feitosContados += Math.min(feitos, meta);
			}
			semanas.add(new SemanaDoHabito(segunda, domingo, meta, feitos, batida, encerrada, conta));
		}

		Double consistencia = esperados == 0 ? null : emPercentual((double) feitosContados / esperados);
		return new HabitoConsistencia(habito.id(), habito.nome(), habito.tipo(), habito.excluidoDia() != null,
				consistencia, semanas, dias);
	}

	private static Estado estadoDoSemanal(HabitoHistorico habito, LocalDate dia, LocalDate hoje, boolean batida,
			boolean encerrada) {
		if (habito.diasFeitos().contains(dia)) {
			return Estado.CONCLUIDO;
		}
		if (dia.isAfter(hoje)) {
			return Estado.DESCANSO_FUTURO;
		}
		if (dia.equals(hoje)) {
			return batida ? Estado.DESCANSO_FUTURO : Estado.EM_ABERTO;
		}
		// Dia passado sem registro: vermelho só se a semana já terminou sem bater a meta (todos os dias dela sem marca).
		return encerrada && !batida ? Estado.NAO_REALIZADO : Estado.DESCANSO_FUTURO;
	}

	private static LocalDate maior(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalDate menor(LocalDate a, LocalDate b) {
		return a.isBefore(b) ? a : b;
	}

	private static Double emPercentual(double proporcao) {
		return BigDecimal.valueOf(proporcao * 100).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

}
