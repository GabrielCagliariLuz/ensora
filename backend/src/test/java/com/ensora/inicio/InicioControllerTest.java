package com.ensora.inicio;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.habito.TipoHabito;
import com.ensora.inicio.dto.InicioResponse;
import com.ensora.inicio.dto.InicioResponse.HabitoNaHome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
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
		when(inicioService.carregar(7L, HOJE)).thenReturn(new InicioResponse(HOJE, List.of(
				new HabitoNaHome(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoNaHome(4L, "Academia", TipoHabito.SEMANAL, false, 1, 3))));

		mvc.perform(get("/inicio").param("data", "2026-10-21").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data").value("2026-10-21"))
				.andExpect(jsonPath("$.habitos[0].nome").value("Ler"))
				.andExpect(jsonPath("$.habitos[0].feito").value(true))
				.andExpect(jsonPath("$.habitos[0].feitos").doesNotExist())
				.andExpect(jsonPath("$.habitos[0].meta").doesNotExist())
				.andExpect(jsonPath("$.habitos[1].feitos").value(1))
				.andExpect(jsonPath("$.habitos[1].meta").value(3));

		verify(inicioService).carregar(7L, HOJE);
	}

	@Test
	void semHabitosDevolveListaVazia() throws Exception {
		when(inicioService.carregar(7L, HOJE)).thenReturn(new InicioResponse(HOJE, List.of()));

		mvc.perform(get("/inicio").param("data", "2026-10-21").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos").isEmpty());
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
