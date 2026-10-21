package com.ensora.insights;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.habito.TipoHabito;
import com.ensora.insights.ConsistenciaCalculadora.Estado;
import com.ensora.insights.dto.ConsistenciaResponse;
import com.ensora.insights.dto.RitmoResponse;
import com.ensora.insights.dto.RitmoResponse.Dia;
import com.ensora.insights.dto.RitmoResponse.Semana;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InsightsController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class InsightsControllerTest {

	private static final LocalDate DE = LocalDate.of(2026, 10, 1);
	private static final LocalDate ATE = LocalDate.of(2026, 10, 3);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private InsightsService insightsService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	@Test
	void devolve200ComRitmoMedioSemanasEDiasEPassaAoServiceOUsuarioDoTokenEOIntervalo() throws Exception {
		when(insightsService.ritmo(7L, DE, ATE)).thenReturn(new RitmoResponse(DE, ATE, 50.0,
				List.of(new Semana("S1", DE, ATE, 50.0)),
				List.of(new Dia(DE, 100.0), new Dia(LocalDate.of(2026, 10, 2), 50.0), new Dia(ATE, 0.0))));

		mvc.perform(get("/insights/ritmo").param("de", "2026-10-01").param("ate", "2026-10-03").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.de").value("2026-10-01"))
				.andExpect(jsonPath("$.ate").value("2026-10-03"))
				.andExpect(jsonPath("$.ritmoMedio").value(50.0))
				.andExpect(jsonPath("$.semanas[0].rotulo").value("S1"))
				.andExpect(jsonPath("$.semanas[0].de").value("2026-10-01"))
				.andExpect(jsonPath("$.semanas[0].ate").value("2026-10-03"))
				.andExpect(jsonPath("$.semanas[0].ritmo").value(50.0))
				.andExpect(jsonPath("$.dias[0].data").value("2026-10-01"))
				.andExpect(jsonPath("$.dias[0].ritmo").value(100.0))
				.andExpect(jsonPath("$.dias[2].ritmo").value(0.0));

		verify(insightsService).ritmo(7L, DE, ATE);
	}

	@Test
	void camposSemValorNaoAparecemNoJson() throws Exception {
		when(insightsService.ritmo(7L, DE, ATE)).thenReturn(new RitmoResponse(DE, ATE, null,
				List.of(new Semana("S1", DE, ATE, null)),
				List.of(new Dia(DE, null))));

		mvc.perform(get("/insights/ritmo").param("de", "2026-10-01").param("ate", "2026-10-03").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ritmoMedio").doesNotExist())
				.andExpect(jsonPath("$.semanas[0].ritmo").doesNotExist())
				.andExpect(jsonPath("$.dias[0].data").value("2026-10-01"))
				.andExpect(jsonPath("$.dias[0].ritmo").doesNotExist());
	}

	@Test
	void ateAntesDeDeDevolve400ComOCampoAte() throws Exception {
		when(insightsService.ritmo(7L, ATE, DE))
				.thenThrow(new ValidacaoException("ate", "não pode ser antes de 'de'"));

		mvc.perform(get("/insights/ritmo").param("de", "2026-10-03").param("ate", "2026-10-01").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("ate"));
	}

	@Test
	void deOuAteAusentesDevolve400SemChamarOService() throws Exception {
		mvc.perform(get("/insights/ritmo").param("ate", "2026-10-03").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/insights/ritmo").param("de", "2026-10-01").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(insightsService);
	}

	@Test
	void datasForaDoFormatoIsoDevolvem400SemChamarOService() throws Exception {
		mvc.perform(get("/insights/ritmo").param("de", "01-10-2026").param("ate", "2026-10-03").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/insights/ritmo").param("de", "2026-10-01").param("ate", "2026-13-45").with(usuario("7")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(insightsService);
	}

	@Test
	void semTokenDevolve401() throws Exception {
		mvc.perform(get("/insights/ritmo").param("de", "2026-10-01").param("ate", "2026-10-03"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(insightsService);
	}

	// ---- consistência ----

	private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

	@Test
	void consistenciaDevolve200ComGradeConsistenciaESemanasEPassaAoServiceOUsuarioDoTokenEOMes() throws Exception {
		ConsistenciaResponse.Habito diario = new ConsistenciaResponse.Habito(1L, "Ler", TipoHabito.DIARIO, false, 33.3,
				List.of(), List.of(
						new ConsistenciaResponse.Dia(LocalDate.of(2026, 9, 10), Estado.CONCLUIDO),
						new ConsistenciaResponse.Dia(LocalDate.of(2026, 9, 17), Estado.NAO_REALIZADO)));
		ConsistenciaResponse.Habito semanal = new ConsistenciaResponse.Habito(4L, "Academia", TipoHabito.SEMANAL, true,
				50.0,
				List.of(new ConsistenciaResponse.Semana(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 13), 2, 2, true,
						true, true)),
				List.of(new ConsistenciaResponse.Dia(LocalDate.of(2026, 9, 8), Estado.EM_ABERTO)));
		when(insightsService.consistencia(7L, SETEMBRO))
				.thenReturn(new ConsistenciaResponse("2026-09", List.of(diario, semanal)));

		mvc.perform(get("/insights/consistencia").param("mes", "2026-09").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mes").value("2026-09"))
				.andExpect(jsonPath("$.habitos[0].nome").value("Ler"))
				.andExpect(jsonPath("$.habitos[0].tipo").value("DIARIO"))
				.andExpect(jsonPath("$.habitos[0].excluido").value(false))
				.andExpect(jsonPath("$.habitos[0].consistencia").value(33.3))
				.andExpect(jsonPath("$.habitos[0].semanas").doesNotExist())
				.andExpect(jsonPath("$.habitos[0].dias[0].data").value("2026-09-10"))
				.andExpect(jsonPath("$.habitos[0].dias[0].estado").value("CONCLUIDO"))
				.andExpect(jsonPath("$.habitos[0].dias[1].estado").value("NAO_REALIZADO"))
				.andExpect(jsonPath("$.habitos[1].excluido").value(true))
				.andExpect(jsonPath("$.habitos[1].semanas[0].de").value("2026-09-07"))
				.andExpect(jsonPath("$.habitos[1].semanas[0].meta").value(2))
				.andExpect(jsonPath("$.habitos[1].semanas[0].batida").value(true))
				.andExpect(jsonPath("$.habitos[1].semanas[0].contaNaConsistencia").value(true))
				.andExpect(jsonPath("$.habitos[1].dias[0].estado").value("EM_ABERTO"));

		verify(insightsService).consistencia(7L, SETEMBRO);
	}

	@Test
	void consistenciaAusenteNaoApareceNoJson() throws Exception {
		ConsistenciaResponse.Habito sem = new ConsistenciaResponse.Habito(1L, "Ler", TipoHabito.DIARIO, false, null,
				List.of(), List.of(new ConsistenciaResponse.Dia(LocalDate.of(2026, 11, 1), Estado.DESCANSO_FUTURO)));
		when(insightsService.consistencia(7L, YearMonth.of(2026, 11)))
				.thenReturn(new ConsistenciaResponse("2026-11", List.of(sem)));

		mvc.perform(get("/insights/consistencia").param("mes", "2026-11").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos[0].consistencia").doesNotExist())
				.andExpect(jsonPath("$.habitos[0].dias[0].estado").value("DESCANSO_FUTURO"));
	}

	@Test
	void consistenciaSemHabitosDevolveListaVazia() throws Exception {
		when(insightsService.consistencia(7L, SETEMBRO)).thenReturn(new ConsistenciaResponse("2026-09", List.of()));

		mvc.perform(get("/insights/consistencia").param("mes", "2026-09").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos").isEmpty());
	}

	@Test
	void consistenciaSemMesOuComMesForaDoFormatoDevolve400SemChamarOService() throws Exception {
		mvc.perform(get("/insights/consistencia").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/insights/consistencia").param("mes", "2026-13").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/insights/consistencia").param("mes", "09-2026").with(usuario("7")))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/insights/consistencia").param("mes", "2026-09-10").with(usuario("7")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(insightsService);
	}

	@Test
	void consistenciaSemTokenDevolve401() throws Exception {
		mvc.perform(get("/insights/consistencia").param("mes", "2026-09")).andExpect(status().isUnauthorized());
		verifyNoInteractions(insightsService);
	}

}
