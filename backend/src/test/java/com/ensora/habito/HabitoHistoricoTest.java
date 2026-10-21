package com.ensora.habito;

import com.ensora.habito.HabitoHistorico.MetaVigente;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HabitoHistoricoTest {

	private static final LocalDate CRIACAO = LocalDate.of(2026, 10, 7);
	private static final LocalDate EXCLUSAO = LocalDate.of(2026, 10, 20);

	private HabitoHistorico diario(LocalDate criadoDia, LocalDate excluidoDia) {
		return new HabitoHistorico(1L, "Ler", TipoHabito.DIARIO, criadoDia, excluidoDia, List.of(), Set.of());
	}

	private HabitoHistorico semanalComMetas(MetaVigente... metas) {
		return new HabitoHistorico(2L, "Academia", TipoHabito.SEMANAL, CRIACAO, null, List.of(metas), Set.of());
	}

	@Test
	void existeNoDiaDaCriacaoMasNaoNoDiaAnterior() {
		HabitoHistorico habito = diario(CRIACAO, null);

		assertThat(habito.existiaEm(CRIACAO)).isTrue();
		assertThat(habito.existiaEm(CRIACAO.minusDays(1))).isFalse();
	}

	@Test
	void existeAteODiaAnteriorAoDaExclusaoMasNaoNoDiaDaExclusao() {
		HabitoHistorico habito = diario(CRIACAO, EXCLUSAO);

		assertThat(habito.existiaEm(EXCLUSAO.minusDays(1))).isTrue();
		assertThat(habito.existiaEm(EXCLUSAO)).isFalse();
		assertThat(habito.existiaEm(EXCLUSAO.plusDays(5))).isFalse();
	}

	@Test
	void semExclusaoExisteParaSempreDepoisDaCriacao() {
		assertThat(diario(CRIACAO, null).existiaEm(LocalDate.of(2030, 1, 1))).isTrue();
	}

	@Test
	void metaNaSemanaUsaAVigenciaMaisRecenteQueJaComecou() {
		LocalDate segundaDaMeta5 = LocalDate.of(2026, 10, 19);
		HabitoHistorico habito = semanalComMetas(
				new MetaVigente(segundaDaMeta5, 5),
				new MetaVigente(LocalDate.of(2026, 10, 5), 3));

		assertThat(habito.metaNaSemana(LocalDate.of(2026, 10, 5))).isEqualTo(3);
		assertThat(habito.metaNaSemana(LocalDate.of(2026, 10, 12))).isEqualTo(3);
		assertThat(habito.metaNaSemana(segundaDaMeta5)).isEqualTo(5);
		assertThat(habito.metaNaSemana(LocalDate.of(2026, 10, 26))).isEqualTo(5);
	}

	@Test
	void metaNaSemanaEhNulaAntesDaPrimeiraVigenciaEParaHabitoSemMetas() {
		HabitoHistorico semanal = semanalComMetas(new MetaVigente(LocalDate.of(2026, 10, 5), 3));

		assertThat(semanal.metaNaSemana(LocalDate.of(2026, 9, 28))).isNull();
		assertThat(diario(CRIACAO, null).metaNaSemana(LocalDate.of(2026, 10, 12))).isNull();
	}

}
