package com.ensora.habito;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.habito.dto.HabitoRequest;
import com.ensora.habito.dto.HabitoResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HabitoController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class HabitoControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private HabitoService habitoService;

	/** Simula um token válido cujo subject (o id do usuário) é o informado. */
	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	private static final String SEMANAL = """
			{"nome": "Academia", "tipo": "SEMANAL", "vezesPorSemana": 3, "data": "2026-10-21"}
			""";

	@Test
	void criarHabitoDevolve201ComOsDadosDoHabito() throws Exception {
		when(habitoService.criar(eq(7L), any())).thenReturn(new HabitoResponse(4L, "Academia", TipoHabito.SEMANAL, 3));

		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(4))
				.andExpect(jsonPath("$.nome").value("Academia"))
				.andExpect(jsonPath("$.tipo").value("SEMANAL"))
				.andExpect(jsonPath("$.vezesPorSemana").value(3));
	}

	@Test
	void criarHabitoPassaAoServiceOIdDoTokenEOCorpoDaRequisicao() throws Exception {
		when(habitoService.criar(eq(7L), any())).thenReturn(new HabitoResponse(4L, "Academia", TipoHabito.SEMANAL, 3));

		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isCreated());

		ArgumentCaptor<HabitoRequest> recebido = ArgumentCaptor.forClass(HabitoRequest.class);
		verify(habitoService).criar(eq(7L), recebido.capture());
		assertThat(recebido.getValue()).isEqualTo(
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, LocalDate.of(2026, 10, 21)));
	}

	@Test
	void habitoDiarioNaoTrazVezesPorSemanaNaResposta() throws Exception {
		when(habitoService.criar(eq(7L), any())).thenReturn(new HabitoResponse(3L, "Beber água", TipoHabito.DIARIO, null));

		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"Beber água\", \"tipo\": \"DIARIO\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vezesPorSemana").doesNotExist());
	}

	@Test
	void nomeEmBrancoDevolve400ComOCampoNome() throws Exception {
		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"  \", \"tipo\": \"DIARIO\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("nome"));
		verifyNoInteractions(habitoService);
	}

	@Test
	void nomeComMaisDeCemCaracteresDevolve400() throws Exception {
		String nomeLongo = "a".repeat(101);

		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"" + nomeLongo + "\", \"tipo\": \"DIARIO\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("nome"));
	}

	@Test
	void vezesPorSemanaOitoDevolve400ComOCampoVezesPorSemana() throws Exception {
		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"Academia\", \"tipo\": \"SEMANAL\", \"vezesPorSemana\": 8, \"data\": \"2026-10-21\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("vezesPorSemana"));
	}

	@Test
	void tipoInexistenteDevolve400DeRequisicaoInvalida() throws Exception {
		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"Academia\", \"tipo\": \"MENSAL\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
	}

	@Test
	void regraDoServiceVira400ComOCampoDaValidacao() throws Exception {
		when(habitoService.criar(eq(7L), any()))
				.thenThrow(new ValidacaoException("vezesPorSemana", "obrigatória para hábito semanal"));

		mvc.perform(post("/habitos").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"Academia\", \"tipo\": \"SEMANAL\", \"data\": \"2026-10-21\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("vezesPorSemana"))
				.andExpect(jsonPath("$.campos[0].mensagem").value("obrigatória para hábito semanal"));
	}

	@Test
	void criarSemTokenDevolve401() throws Exception {
		mvc.perform(post("/habitos").contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
		verifyNoInteractions(habitoService);
	}

	@Test
	void listarDevolve200ComOsHabitosDoUsuarioDoToken() throws Exception {
		when(habitoService.listar(7L)).thenReturn(List.of(
				new HabitoResponse(3L, "Beber água", TipoHabito.DIARIO, null),
				new HabitoResponse(4L, "Academia", TipoHabito.SEMANAL, 3)));

		mvc.perform(get("/habitos").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].nome").value("Beber água"))
				.andExpect(jsonPath("$[0].vezesPorSemana").doesNotExist())
				.andExpect(jsonPath("$[1].vezesPorSemana").value(3));
		verify(habitoService).listar(7L);
	}

	@Test
	void listarUsaOIdDoTokenDeQuemChamou() throws Exception {
		when(habitoService.listar(8L)).thenReturn(List.of());

		mvc.perform(get("/habitos").with(usuario("8")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
		verify(habitoService).listar(8L);
	}

	@Test
	void listarSemTokenDevolve401() throws Exception {
		mvc.perform(get("/habitos"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
		verifyNoInteractions(habitoService);
	}

	@Test
	void tokenComSubjectQueNaoEhNumeroDevolve401() throws Exception {
		mvc.perform(get("/habitos").with(usuario("abc")))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(habitoService);
	}

	// ---- edição e exclusão ----

	@Test
	void atualizarDevolve200ComOHabitoAtualizado() throws Exception {
		when(habitoService.atualizar(eq(7L), eq(4L), any()))
				.thenReturn(new HabitoResponse(4L, "Academia", TipoHabito.SEMANAL, 3));

		mvc.perform(put("/habitos/4").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4))
				.andExpect(jsonPath("$.vezesPorSemana").value(3));
	}

	@Test
	void atualizarPassaAoServiceOIdDoUsuarioDoTokenOIdDaUrlEOCorpo() throws Exception {
		when(habitoService.atualizar(eq(7L), eq(4L), any()))
				.thenReturn(new HabitoResponse(4L, "Academia", TipoHabito.SEMANAL, 3));

		mvc.perform(put("/habitos/4").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isOk());

		ArgumentCaptor<HabitoRequest> recebido = ArgumentCaptor.forClass(HabitoRequest.class);
		verify(habitoService).atualizar(eq(7L), eq(4L), recebido.capture());
		assertThat(recebido.getValue()).isEqualTo(
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, LocalDate.of(2026, 10, 21)));
	}

	@Test
	void atualizarHabitoDeOutroUsuarioDevolve404() throws Exception {
		when(habitoService.atualizar(eq(7L), eq(4L), any()))
				.thenThrow(new RecursoNaoEncontradoException("Hábito não encontrado"));

		mvc.perform(put("/habitos/4").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
				.andExpect(jsonPath("$.mensagem").value("Hábito não encontrado"));
	}

	@Test
	void tentarMudarOTipoDevolve400ComOCampoTipo() throws Exception {
		when(habitoService.atualizar(eq(7L), eq(4L), any()))
				.thenThrow(new ValidacaoException("tipo", "não pode ser alterado"));

		mvc.perform(put("/habitos/4").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("tipo"));
	}

	@Test
	void atualizarComNomeEmBrancoDevolve400SemChamarOService() throws Exception {
		mvc.perform(put("/habitos/4").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"\", \"tipo\": \"DIARIO\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("nome"));
		verifyNoInteractions(habitoService);
	}

	@Test
	void atualizarSemTokenDevolve401() throws Exception {
		mvc.perform(put("/habitos/4").contentType(MediaType.APPLICATION_JSON).content(SEMANAL))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(habitoService);
	}

	@Test
	void excluirDevolve204EPassaAoServiceOUsuarioDoTokenEOIdDaUrl() throws Exception {
		mvc.perform(delete("/habitos/4").with(usuario("7")))
				.andExpect(status().isNoContent());

		verify(habitoService).excluir(7L, 4L);
	}

	@Test
	void excluirHabitoDeOutroUsuarioDevolve404() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Hábito não encontrado")).when(habitoService).excluir(7L, 4L);

		mvc.perform(delete("/habitos/4").with(usuario("7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void excluirSemTokenDevolve401() throws Exception {
		mvc.perform(delete("/habitos/4"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(habitoService);
	}

	@Test
	void idQueNaoEhNumeroDevolve400() throws Exception {
		mvc.perform(delete("/habitos/abc").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(habitoService);
	}

}
