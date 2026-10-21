package com.ensora.insights;

import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.HabitoService;
import com.ensora.habito.TipoHabito;
import com.ensora.insights.ConsistenciaCalculadora.Estado;
import com.ensora.insights.dto.ConsistenciaResponse;
import com.ensora.insights.dto.RitmoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

	private static final Long USUARIO = 7L;
	private static final LocalDate DE = LocalDate.of(2026, 10, 1);

	@Mock
	private HabitoService habitoService;

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	private InsightsService servico(Instant agora) {
		return new InsightsService(habitoService, Clock.fixed(agora, ZoneOffset.UTC), SAO_PAULO);
	}

	private InsightsService servico() {
		return servico(Instant.parse("2026-10-21T15:00:00Z"));
	}

	private void assertErroNoCampoAte(LocalDate de, LocalDate ate) {
		assertThatThrownBy(() -> servico().ritmo(USUARIO, de, ate))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("ate"));
		verifyNoInteractions(habitoService);
	}

	@Test
	void ritmoCalculaComOHistoricoDoUsuarioEDevolveDiasSemanasEMedia() {
		LocalDate ate = LocalDate.of(2026, 10, 3);
		HabitoHistorico leitura = new HabitoHistorico(1L, "Ler", TipoHabito.DIARIO, LocalDate.of(2026, 9, 1), null,
				List.of(), Set.of(DE, LocalDate.of(2026, 10, 2)));
		when(habitoService.historicoParaInsights(USUARIO, DE, ate)).thenReturn(List.of(leitura));

		RitmoResponse resposta = servico().ritmo(USUARIO, DE, ate);

		assertThat(resposta.de()).isEqualTo(DE);
		assertThat(resposta.ate()).isEqualTo(ate);
		assertThat(resposta.dias()).extracting(RitmoResponse.Dia::ritmo).containsExactly(100.0, 100.0, 0.0);
		assertThat(resposta.semanas()).hasSize(1);
		assertThat(resposta.semanas().get(0).rotulo()).isEqualTo("S1");
		assertThat(resposta.ritmoMedio()).isEqualTo(66.7);
		verify(habitoService).historicoParaInsights(USUARIO, DE, ate);
	}

	@Test
	void ritmoSemHabitosDevolveDiasSemValorEMediaNula() {
		LocalDate ate = LocalDate.of(2026, 10, 2);
		when(habitoService.historicoParaInsights(USUARIO, DE, ate)).thenReturn(List.of());

		RitmoResponse resposta = servico().ritmo(USUARIO, DE, ate);

		assertThat(resposta.ritmoMedio()).isNull();
		assertThat(resposta.dias()).extracting(RitmoResponse.Dia::ritmo).containsExactly(null, null);
	}

	@Test
	void ritmoAceitaOMesInteiroDe31Dias() {
		LocalDate ate = DE.plusDays(30);
		when(habitoService.historicoParaInsights(USUARIO, DE, ate)).thenReturn(List.of());

		RitmoResponse resposta = servico().ritmo(USUARIO, DE, ate);

		assertThat(resposta.dias()).hasSize(31);
		assertThat(resposta.semanas()).hasSize(4);
	}

	@Test
	void ritmoAceitaIntervaloDeUmDia() {
		when(habitoService.historicoParaInsights(USUARIO, DE, DE)).thenReturn(List.of());

		assertThat(servico().ritmo(USUARIO, DE, DE).dias()).hasSize(1);
	}

	@Test
	void ritmoComIntervaloDe32DiasLancaValidacaoNoCampoAteSemConsultar() {
		assertErroNoCampoAte(DE, DE.plusDays(31));
	}

	@Test
	void ritmoComAteAntesDeDeLancaValidacaoNoCampoAteSemConsultar() {
		assertErroNoCampoAte(DE, DE.minusDays(1));
	}

	// ---- consistência ----

	private HabitoHistorico diarioCriadoEm(LocalDate criadoDia, LocalDate... feitos) {
		return new HabitoHistorico(1L, "Ler", TipoHabito.DIARIO, criadoDia, null, List.of(), Set.of(feitos));
	}

	@Test
	void consistenciaPedeOHistoricoDeSemanasInteirasDoMes() {
		// Setembro/2026: 1º é terça (segunda 31/08) e 30 é quarta (semana termina em domingo 04/10).
		when(habitoService.historicoParaInsights(USUARIO, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 10, 4)))
				.thenReturn(List.of());

		ConsistenciaResponse resposta = servico().consistencia(USUARIO, YearMonth.of(2026, 9));

		assertThat(resposta.mes()).isEqualTo("2026-09");
		assertThat(resposta.habitos()).isEmpty();
		verify(habitoService).historicoParaInsights(USUARIO, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 10, 4));
	}

	@Test
	void consistenciaDeMesQueTerminaNoSabadoVaiAteODomingoSeguinte() {
		// Outubro/2026: 1º é quinta (segunda 28/09) e 31 é sábado (semana termina em domingo 01/11).
		when(habitoService.historicoParaInsights(USUARIO, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 1)))
				.thenReturn(List.of());

		servico().consistencia(USUARIO, YearMonth.of(2026, 10));

		verify(habitoService).historicoParaInsights(USUARIO, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 1));
	}

	@Test
	void consistenciaCalculaComOHistoricoDoUsuarioEMontaARespostaDaGrade() {
		LocalDate criado = LocalDate.of(2026, 9, 1);
		when(habitoService.historicoParaInsights(USUARIO, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 10, 4)))
				.thenReturn(List.of(diarioCriadoEm(criado, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2))));

		ConsistenciaResponse resposta = servico().consistencia(USUARIO, YearMonth.of(2026, 9));

		assertThat(resposta.habitos()).hasSize(1);
		ConsistenciaResponse.Habito habito = resposta.habitos().get(0);
		assertThat(habito.nome()).isEqualTo("Ler");
		assertThat(habito.consistencia()).isEqualTo(6.7); // 2 feitos em 30 dias
		assertThat(habito.dias()).hasSize(30);
		assertThat(habito.dias().get(0).estado()).isEqualTo(Estado.CONCLUIDO);
		assertThat(habito.dias().get(2).estado()).isEqualTo(Estado.NAO_REALIZADO);
		assertThat(habito.semanas()).isEmpty();
	}

	@Test
	void consistenciaUsaOHojeDoFusoDoServidorENaoDoUtc() {
		// 21/10 às 02:00 UTC ainda é 20/10 às 23:00 em São Paulo: hoje é dia 20.
		when(habitoService.historicoParaInsights(USUARIO, LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 1)))
				.thenReturn(List.of(diarioCriadoEm(LocalDate.of(2026, 9, 1))));

		ConsistenciaResponse resposta = servico(Instant.parse("2026-10-21T02:00:00Z"))
				.consistencia(USUARIO, YearMonth.of(2026, 10));

		var dias = resposta.habitos().get(0).dias();
		assertThat(dias.get(19).data()).isEqualTo(LocalDate.of(2026, 10, 20));
		assertThat(dias.get(19).estado()).isEqualTo(Estado.EM_ABERTO);
		assertThat(dias.get(20).estado()).isEqualTo(Estado.DESCANSO_FUTURO);
	}

}
