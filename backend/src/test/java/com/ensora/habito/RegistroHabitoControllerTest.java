package com.ensora.habito;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistroHabitoController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class RegistroHabitoControllerTest {

	private static final LocalDate DATA = LocalDate.of(2026, 10, 21);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private RegistroHabitoService registroHabitoService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	@Test
	void marcarDevolve204EPassaAoServiceOUsuarioDoTokenOHabitoEADataDaUrl() throws Exception {
		mvc.perform(put("/habitos/4/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isNoContent());

		verify(registroHabitoService).marcar(7L, 4L, DATA);
	}

	@Test
	void desmarcarDevolve204EPassaAoServiceOUsuarioDoTokenOHabitoEADataDaUrl() throws Exception {
		mvc.perform(delete("/habitos/4/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isNoContent());

		verify(registroHabitoService).desmarcar(7L, 4L, DATA);
	}

	@Test
	void marcarHabitoDeOutroUsuarioDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Hábito não encontrado"))
				.when(registroHabitoService).marcar(7L, 4L, DATA);

		mvc.perform(put("/habitos/4/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void desmarcarHabitoDeOutroUsuarioDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Hábito não encontrado"))
				.when(registroHabitoService).desmarcar(7L, 4L, DATA);

		mvc.perform(delete("/habitos/4/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void marcarDataFuturaDevolve400ComOCampoData() throws Exception {
		doThrow(new ValidacaoException("data", "não pode ser uma data futura"))
				.when(registroHabitoService).marcar(7L, 4L, DATA);

		mvc.perform(put("/habitos/4/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("data"));
	}

	@Test
	void dataForaDoFormatoIsoDevolve400SemChamarOService() throws Exception {
		mvc.perform(put("/habitos/4/registros/21-10-2026").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		mvc.perform(put("/habitos/4/registros/2026-13-45").with(usuario("7")))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(registroHabitoService);
	}

	@Test
	void idDoHabitoQueNaoEhNumeroDevolve400() throws Exception {
		mvc.perform(delete("/habitos/abc/registros/2026-10-21").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(registroHabitoService);
	}

	@Test
	void marcarEDesmarcarSemTokenDevolvem401() throws Exception {
		mvc.perform(put("/habitos/4/registros/2026-10-21")).andExpect(status().isUnauthorized());
		mvc.perform(delete("/habitos/4/registros/2026-10-21")).andExpect(status().isUnauthorized());
		verifyNoInteractions(registroHabitoService);
	}

}
