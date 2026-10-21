package com.ensora.inicio;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.entrega.dto.EntregaResponse;
import com.ensora.evento.CategoriaEvento;
import com.ensora.evento.dto.EventoResponse;
import com.ensora.habito.TipoHabito;
import com.ensora.inicio.dto.InicioResponse;
import com.ensora.inicio.dto.InicioResponse.HabitoNaHome;
import com.ensora.inicio.dto.InicioResponse.Upcoming;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InicioController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class InicioControllerTest {

	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private InicioService inicioService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	@Test
	void devolve200ComOsHabitosEPassaAoServiceOUsuarioDoTokenEADataDaUrl() throws Exception {
		EntregaResponse entrega = new EntregaResponse(3L, "Trabalho", HOJE.plusDays(1), LocalTime.of(14, 30), false,
				null, null, "Cap. 1");
		EntregaResponse concluida = new EntregaResponse(5L, "Lista", HOJE.minusDays(1), null, true,
				Instant.parse("2026-10-21T15:04:05Z"), HOJE, null);
		EventoResponse evento = new EventoResponse(8L, "Consulta", HOJE.plusDays(2), LocalTime.of(9, 0),
				LocalTime.of(10, 0), "Clínica", CategoriaEvento.SAUDE);
		when(inicioService.carregar(7L, HOJE)).thenReturn(new InicioResponse(HOJE, List.of(
				new HabitoNaHome(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoNaHome(4L, "Academia", TipoHabito.SEMANAL, false, 1, 3)),
				new Upcoming(List.of(entrega, concluida), List.of(evento))));

		mvc.perform(get("/inicio").param("data", "2026-10-21").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value("2026-10-21"))
				.andExpect(jsonPath("$.habitos[0].nome").value("Ler"))
				.andExpect(jsonPath("$.habitos[0].feito").value(true))
				.andExpect(jsonPath("$.habitos[0].feitos").doesNotExist())
				.andExpect(jsonPath("$.habitos[0].meta").doesNotExist())
				.andExpect(jsonPath("$.habitos[1].feitos").value(1))
				.andExpect(jsonPath("$.habitos[1].meta").value(3))
				.andExpect(jsonPath("$.upcoming.entregas[0].titulo").value("Trabalho"))
				.andExpect(jsonPath("$.upcoming.entregas[0].prazoData").value("2026-10-22"))
				.andExpect(jsonPath("$.upcoming.entregas[0].prazoHorario").value("14:30:00"))
				.andExpect(jsonPath("$.upcoming.entregas[0].concluida").value(false))
				.andExpect(jsonPath("$.upcoming.entregas[0].concluidaEm").doesNotExist())
				.andExpect(jsonPath("$.upcoming.entregas[1].concluida").value(true))
				.andExpect(jsonPath("$.upcoming.entregas[1].concluidaEm").value("2026-10-21T15:04:05Z"))
				.andExpect(jsonPath("$.upcoming.entregas[1].concluidaData").value("2026-10-21"))
				.andExpect(jsonPath("$.upcoming.entregas[1].prazoHorario").doesNotExist())
				.andExpect(jsonPath("$.upcoming.eventos[0].titulo").value("Consulta"))
				.andExpect(jsonPath("$.upcoming.eventos[0].inicio").value("09:00:00"))
				.andExpect(jsonPath("$.upcoming.eventos[0].categoria").value("SAUDE"));

		verify(inicioService).carregar(7L, HOJE);
	}

	@Test
	void semNadaDevolveListasVazias() throws Exception {
		when(inicioService.carregar(7L, HOJE))
				.thenReturn(new InicioResponse(HOJE, List.of(), new Upcoming(List.of(), List.of())));

		mvc.perform(get("/inicio").param("data", "2026-10-21").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos").isEmpty())
				.andExpect(jsonPath("$.upcoming.entregas").isEmpty())
				.andExpect(jsonPath("$.upcoming.eventos").isEmpty());
	}

	@Test
	void dataForaDaToleranciaDevolve400ComOCampoData() throws Exception {
		when(inicioService.carregar(7L, HOJE))
				.thenThrow(new ValidacaoException("data", "muito distante da data de hoje"));

		mvc.perform(get("/inicio").param("data", "2026-10-21").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("data"));
	}

	@Test
	void dataAusenteOuForaDoFormatoIsoDevolve400SemChamarOService() throws Exception {
		mvc.perform(get("/inicio").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/inicio").param("data", "21-10-2026").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(inicioService);
	}

	@Test
	void semTokenDevolve401() throws Exception {
		mvc.perform(get("/inicio").param("data", "2026-10-21")).andExpect(status().isUnauthorized());
		verifyNoInteractions(inicioService);
	}

}
