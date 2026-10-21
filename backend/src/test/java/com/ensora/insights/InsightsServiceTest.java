package com.ensora.insights;

import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.HabitoService;
import com.ensora.habito.TipoHabito;
import com.ensora.insights.dto.RitmoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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

	private InsightsService servico() {
		return new InsightsService(habitoService);
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

}
