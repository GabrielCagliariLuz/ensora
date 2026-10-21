package com.ensora.evento;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.evento.dto.EventoRequest;
import com.ensora.evento.dto.EventoResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventoController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class EventoControllerTest {

	private static final LocalDate DATA = LocalDate.of(2026, 11, 5);
	private static final String CORPO = """
			{"titulo": "Consulta", "data": "2026-11-05", "inicio": "09:00", "termino": "10:00",
			 "local": "Clínica", "categoria": "SAUDE"}
			""";
	private static final String MINIMO = "{\"titulo\": \"Reunião\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\"}";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EventoService eventoService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	private EventoResponse resposta() {
		return new EventoResponse(3L, "Consulta", DATA, LocalTime.of(9, 0), LocalTime.of(10, 0), "Clínica",
				CategoriaEvento.SAUDE);
	}

	// ---- POST ----

	@Test
	void criarDevolve201ComOsDadosDoEvento() throws Exception {
		when(eventoService.criar(eq(7L), any())).thenReturn(resposta());

		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.titulo").value("Consulta"))
				.andExpect(jsonPath("$.data").value("2026-11-05"))
				.andExpect(jsonPath("$.inicio").value("09:00:00"))
				.andExpect(jsonPath("$.termino").value("10:00:00"))
				.andExpect(jsonPath("$.local").value("Clínica"))
				.andExpect(jsonPath("$.categoria").value("SAUDE"));
	}

	@Test
	void criarPassaAoServiceOUsuarioDoTokenEOCorpo() throws Exception {
		when(eventoService.criar(eq(7L), any())).thenReturn(resposta());

		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated());

		ArgumentCaptor<EventoRequest> recebido = ArgumentCaptor.forClass(EventoRequest.class);
		verify(eventoService).criar(eq(7L), recebido.capture());
		assertThat(recebido.getValue()).isEqualTo(new EventoRequest("Consulta", DATA, LocalTime.of(9, 0),
				LocalTime.of(10, 0), "Clínica", CategoriaEvento.SAUDE));
	}

	@Test
	void criarSoComTituloDataEInicioDevolve201SemOsCamposOpcionais() throws Exception {
		when(eventoService.criar(eq(7L), any())).thenReturn(
				new EventoResponse(4L, "Reunião", DATA, LocalTime.of(9, 0), null, null, null));

		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(MINIMO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.termino").doesNotExist())
				.andExpect(jsonPath("$.local").doesNotExist())
				.andExpect(jsonPath("$.categoria").doesNotExist());
	}

	@Test
	void todasAsCategoriasDaListaSaoAceitas() throws Exception {
		when(eventoService.criar(eq(7L), any())).thenReturn(resposta());

		for (CategoriaEvento categoria : CategoriaEvento.values()) {
			mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
							.content("{\"titulo\": \"X\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\", "
									+ "\"categoria\": \"" + categoria.name() + "\"}"))
					.andExpect(status().isCreated());
		}
	}

	@Test
	void categoriaForaDaListaDevolve400SemChamarOService() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"X\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\", "
								+ "\"categoria\": \"AZUL\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void terminoNaoPosteriorAoInicioDevolve400ComOCampoTermino() throws Exception {
		when(eventoService.criar(eq(7L), any()))
				.thenThrow(new ValidacaoException("termino", "deve ser depois do início"));

		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("termino"));
	}

	@Test
	void tituloEmBrancoDevolve400ComOCampoTitulo() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"  \", \"data\": \"2026-11-05\", \"inicio\": \"09:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void tituloComMaisDe150CaracteresDevolve400() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"" + "a".repeat(151) + "\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void localComMaisDe100CaracteresDevolve400() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"X\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\", \"local\": \""
								+ "a".repeat(101) + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("local"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void semDataDevolve400ComOCampoData() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"X\", \"inicio\": \"09:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("data"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void semInicioDevolve400ComOCampoInicio() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"X\", \"data\": \"2026-11-05\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("inicio"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void horaForaDoFormatoDevolve400SemChamarOService() throws Exception {
		mvc.perform(post("/eventos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"X\", \"data\": \"2026-11-05\", \"inicio\": \"25:99\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void criarSemTokenDevolve401() throws Exception {
		mvc.perform(post("/eventos").contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(eventoService);
	}

	// ---- PUT ----

	@Test
	void atualizarDevolve200EPassaAoServiceUsuarioIdECorpo() throws Exception {
		when(eventoService.atualizar(eq(7L), eq(3L), any())).thenReturn(resposta());

		mvc.perform(put("/eventos/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3));

		ArgumentCaptor<EventoRequest> recebido = ArgumentCaptor.forClass(EventoRequest.class);
		verify(eventoService).atualizar(eq(7L), eq(3L), recebido.capture());
		assertThat(recebido.getValue().titulo()).isEqualTo("Consulta");
		assertThat(recebido.getValue().categoria()).isEqualTo(CategoriaEvento.SAUDE);
	}

	@Test
	void atualizarEventoDeOutroUsuarioDevolve404() throws Exception {
		when(eventoService.atualizar(eq(7L), eq(3L), any()))
				.thenThrow(new RecursoNaoEncontradoException("Evento não encontrado"));

		mvc.perform(put("/eventos/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void atualizarComTerminoInvalidoDevolve400ComOCampoTermino() throws Exception {
		when(eventoService.atualizar(eq(7L), eq(3L), any()))
				.thenThrow(new ValidacaoException("termino", "deve ser depois do início"));

		mvc.perform(put("/eventos/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("termino"));
	}

	@Test
	void atualizarComCorpoInvalidoDevolve400SemChamarOService() throws Exception {
		mvc.perform(put("/eventos/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"\", \"data\": \"2026-11-05\", \"inicio\": \"09:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void atualizarSemTokenDevolve401() throws Exception {
		mvc.perform(put("/eventos/3").contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(eventoService);
	}

	// ---- DELETE ----

	@Test
	void excluirDevolve204EPassaAoServiceUsuarioEId() throws Exception {
		mvc.perform(delete("/eventos/3").with(usuario("7")))
				.andExpect(status().isNoContent());

		verify(eventoService).excluir(7L, 3L);
	}

	@Test
	void excluirEventoDeOutroUsuarioDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Evento não encontrado"))
				.when(eventoService).excluir(7L, 3L);

		mvc.perform(delete("/eventos/3").with(usuario("7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void idQueNaoEhNumeroDevolve400() throws Exception {
		mvc.perform(delete("/eventos/abc").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(eventoService);
	}

	@Test
	void excluirSemTokenDevolve401() throws Exception {
		mvc.perform(delete("/eventos/3")).andExpect(status().isUnauthorized());
		verifyNoInteractions(eventoService);
	}

}
