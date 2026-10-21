package com.ensora.calendario;

import com.ensora.calendario.dto.CalendarioResponse;
import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.entrega.dto.EntregaResponse;
import com.ensora.evento.CategoriaEvento;
import com.ensora.evento.dto.EventoResponse;
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

@WebMvcTest(CalendarioController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class CalendarioControllerTest {

	private static final LocalDate DE = LocalDate.of(2026, 10, 25);
	private static final LocalDate ATE = LocalDate.of(2026, 12, 5);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private CalendarioService calendarioService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	@Test
	void devolve200ComEntregasEEventosEPassaAoServiceOUsuarioDoTokenEOIntervalo() throws Exception {
		LocalDate dia = LocalDate.of(2026, 11, 5);
		EntregaResponse pendente = new EntregaResponse(3L, "Trabalho", dia, LocalTime.of(14, 30), false, null, null,
				"Cap. 1");
		EntregaResponse concluida = new EntregaResponse(5L, "Lista", dia, null, true,
				Instant.parse("2026-11-04T15:04:05Z"), LocalDate.of(2026, 11, 4), null);
		EventoResponse evento = new EventoResponse(8L, "Consulta", dia, LocalTime.of(9, 0), LocalTime.of(10, 0),
				"Clínica", CategoriaEvento.SAUDE);
		when(calendarioService.carregar(7L, DE, ATE))
				.thenReturn(new CalendarioResponse(DE, ATE, List.of(pendente, concluida), List.of(evento)));

		mvc.perform(get("/calendario").param("de", "2026-10-25").param("ate", "2026-12-05").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.de").value("2026-10-25"))
				.andExpect(jsonPath("$.ate").value("2026-12-05"))
				.andExpect(jsonPath("$.entregas[0].titulo").value("Trabalho"))
				.andExpect(jsonPath("$.entregas[0].prazoHorario").value("14:30:00"))
				.andExpect(jsonPath("$.entregas[0].concluida").value(false))
				.andExpect(jsonPath("$.entregas[0].concluidaEm").doesNotExist())
				.andExpect(jsonPath("$.entregas[1].concluida").value(true))
				.andExpect(jsonPath("$.entregas[1].concluidaData").value("2026-11-04"))
				.andExpect(jsonPath("$.eventos[0].titulo").value("Consulta"))
				.andExpect(jsonPath("$.eventos[0].inicio").value("09:00:00"))
				.andExpect(jsonPath("$.eventos[0].categoria").value("SAUDE"));

		verify(calendarioService).carregar(7L, DE, ATE);
	}

	@Test
	void semNadaDevolveListasVazias() throws Exception {
		when(calendarioService.carregar(7L, DE, ATE))
				.thenReturn(new CalendarioResponse(DE, ATE, List.of(), List.of()));

		mvc.perform(get("/calendario").param("de", "2026-10-25").param("ate", "2026-12-05").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregas").isEmpty())
				.andExpect(jsonPath("$.eventos").isEmpty());
	}

	@Test
	void ateAntesDeDeDevolve400ComOCampoAte() throws Exception {
		when(calendarioService.carregar(7L, ATE, DE))
				.thenThrow(new ValidacaoException("ate", "não pode ser antes de 'de'"));

		mvc.perform(get("/calendario").param("de", "2026-12-05").param("ate", "2026-10-25").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("ate"));
	}

	@Test
	void deOuAteAusentesDevolve400SemChamarOService() throws Exception {
		mvc.perform(get("/calendario").param("ate", "2026-12-05").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/calendario").param("de", "2026-10-25").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(calendarioService);
	}

	@Test
	void datasForaDoFormatoIsoDevolvem400SemChamarOService() throws Exception {
		mvc.perform(get("/calendario").param("de", "25-10-2026").param("ate", "2026-12-05").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(get("/calendario").param("de", "2026-10-25").param("ate", "2026-13-45").with(usuario("7")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(calendarioService);
	}

	@Test
	void semTokenDevolve401() throws Exception {
		mvc.perform(get("/calendario").param("de", "2026-10-25").param("ate", "2026-12-05"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(calendarioService);
	}

}
