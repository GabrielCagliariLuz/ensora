package com.ensora.entrega;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.entrega.dto.EntregaRequest;
import com.ensora.entrega.dto.EntregaResponse;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class EntregaControllerTest {

	private static final LocalDate PRAZO = LocalDate.of(2026, 11, 5);
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
	private static final String CORPO = """
			{"titulo": "Trabalho de Cálculo", "prazoData": "2026-11-05", "prazoHorario": "14:30", "nota": "Cap. 1 a 4"}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private EntregaService entregaService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	private EntregaResponse resposta(boolean concluida, Instant concluidaEm, LocalDate concluidaData) {
		return new EntregaResponse(3L, "Trabalho de Cálculo", PRAZO, LocalTime.of(14, 30), concluida, concluidaEm,
				concluidaData, "Cap. 1 a 4");
	}

	private EntregaResponse resposta(boolean concluida, Instant concluidaEm) {
		return resposta(concluida, concluidaEm, null);
	}

	// ---- POST ----

	@Test
	void criarDevolve201ComOsDadosDaEntrega() throws Exception {
		when(entregaService.criar(eq(7L), any())).thenReturn(resposta(false, null));

		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(3))
				.andExpect(jsonPath("$.titulo").value("Trabalho de Cálculo"))
				.andExpect(jsonPath("$.prazoData").value("2026-11-05"))
				.andExpect(jsonPath("$.prazoHorario").value("14:30:00"))
				.andExpect(jsonPath("$.concluida").value(false))
				.andExpect(jsonPath("$.concluidaEm").doesNotExist())
				.andExpect(jsonPath("$.nota").value("Cap. 1 a 4"));
	}

	@Test
	void criarPassaAoServiceOUsuarioDoTokenEOCorpo() throws Exception {
		when(entregaService.criar(eq(7L), any())).thenReturn(resposta(false, null));

		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isCreated());

		ArgumentCaptor<EntregaRequest> recebido = ArgumentCaptor.forClass(EntregaRequest.class);
		verify(entregaService).criar(eq(7L), recebido.capture());
		assertThat(recebido.getValue()).isEqualTo(
				new EntregaRequest("Trabalho de Cálculo", PRAZO, LocalTime.of(14, 30), "Cap. 1 a 4"));
	}

	@Test
	void criarSoComTituloEPrazoDevolve201SemHorarioNemNota() throws Exception {
		when(entregaService.criar(eq(7L), any()))
				.thenReturn(new EntregaResponse(4L, "Prova", PRAZO, null, false, null, null, null));

		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"Prova\", \"prazoData\": \"2026-11-05\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.prazoHorario").doesNotExist())
				.andExpect(jsonPath("$.nota").doesNotExist());
	}

	@Test
	void tituloEmBrancoDevolve400ComOCampoTitulo() throws Exception {
		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"  \", \"prazoData\": \"2026-11-05\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void tituloComMaisDe150CaracteresDevolve400() throws Exception {
		String titulo = "a".repeat(151);

		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"" + titulo + "\", \"prazoData\": \"2026-11-05\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void notaComMaisDe1000CaracteresDevolve400() throws Exception {
		String nota = "a".repeat(1001);

		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"Prova\", \"prazoData\": \"2026-11-05\", \"nota\": \"" + nota + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("nota"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void semPrazoDataDevolve400ComOCampoPrazoData() throws Exception {
		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"Prova\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("prazoData"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void horarioForaDoFormatoDevolve400SemChamarOService() throws Exception {
		mvc.perform(post("/entregas").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"Prova\", \"prazoData\": \"2026-11-05\", \"prazoHorario\": \"25:99\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void criarSemTokenDevolve401() throws Exception {
		mvc.perform(post("/entregas").contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(entregaService);
	}

	// ---- PUT ----

	@Test
	void atualizarDevolve200EPassaAoServiceUsuarioIdECorpo() throws Exception {
		when(entregaService.atualizar(eq(7L), eq(3L), any())).thenReturn(resposta(false, null));

		mvc.perform(put("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(3));

		ArgumentCaptor<EntregaRequest> recebido = ArgumentCaptor.forClass(EntregaRequest.class);
		verify(entregaService).atualizar(eq(7L), eq(3L), recebido.capture());
		assertThat(recebido.getValue().titulo()).isEqualTo("Trabalho de Cálculo");
	}

	@Test
	void atualizarEntregaDeOutroUsuarioDevolve404() throws Exception {
		when(entregaService.atualizar(eq(7L), eq(3L), any()))
				.thenThrow(new RecursoNaoEncontradoException("Entrega não encontrada"));

		mvc.perform(put("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void atualizarComCorpoInvalidoDevolve400SemChamarOService() throws Exception {
		mvc.perform(put("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"titulo\": \"\", \"prazoData\": \"2026-11-05\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("titulo"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void atualizarSemTokenDevolve401() throws Exception {
		mvc.perform(put("/entregas/3").contentType(MediaType.APPLICATION_JSON).content(CORPO))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(entregaService);
	}

	// ---- PATCH ----

	@Test
	void concluirDevolve200ComConcluidaEmEConcluidaDataEPassaBooleanoEDataAoService() throws Exception {
		Instant em = Instant.parse("2026-10-21T15:04:05Z");
		when(entregaService.alterarConclusao(7L, 3L, true, HOJE)).thenReturn(resposta(true, em, HOJE));

		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"concluida\": true, \"data\": \"2026-10-21\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.concluida").value(true))
				.andExpect(jsonPath("$.concluidaEm").value("2026-10-21T15:04:05Z"))
				.andExpect(jsonPath("$.concluidaData").value("2026-10-21"));

		verify(entregaService).alterarConclusao(7L, 3L, true, HOJE);
	}

	@Test
	void desfazerDevolve200SemConcluidaEmNemConcluidaDataEPassaDataNulaAoService() throws Exception {
		when(entregaService.alterarConclusao(7L, 3L, false, null)).thenReturn(resposta(false, null, null));

		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"concluida\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.concluida").value(false))
				.andExpect(jsonPath("$.concluidaEm").doesNotExist())
				.andExpect(jsonPath("$.concluidaData").doesNotExist());

		verify(entregaService).alterarConclusao(7L, 3L, false, null);
	}

	@Test
	void concluirSemDataDevolve400ComOCampoDataVindoDoService() throws Exception {
		when(entregaService.alterarConclusao(7L, 3L, true, null))
				.thenThrow(new ValidacaoException("data", "obrigatória ao concluir"));

		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"concluida\": true}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("data"));
	}

	@Test
	void dataForaDoFormatoIsoNoPatchDevolve400SemChamarOService() throws Exception {
		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"concluida\": true, \"data\": \"21-10-2026\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void alterarConclusaoDeEntregaDeOutroUsuarioDevolve404() throws Exception {
		when(entregaService.alterarConclusao(7L, 3L, true, HOJE))
				.thenThrow(new RecursoNaoEncontradoException("Entrega não encontrada"));

		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"concluida\": true, \"data\": \"2026-10-21\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void concluidaAusenteDevolve400ComOCampoConcluida() throws Exception {
		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("concluida"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void patchSemCorpoDevolve400() throws Exception {
		mvc.perform(patch("/entregas/3").with(usuario("7")).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void patchSemTokenDevolve401() throws Exception {
		mvc.perform(patch("/entregas/3").contentType(MediaType.APPLICATION_JSON).content("{\"concluida\": true}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(entregaService);
	}

	// ---- DELETE ----

	@Test
	void excluirDevolve204EPassaAoServiceUsuarioEId() throws Exception {
		mvc.perform(delete("/entregas/3").with(usuario("7")))
				.andExpect(status().isNoContent());

		verify(entregaService).excluir(7L, 3L);
	}

	@Test
	void excluirEntregaDeOutroUsuarioDevolve404() throws Exception {
		org.mockito.Mockito.doThrow(new RecursoNaoEncontradoException("Entrega não encontrada"))
				.when(entregaService).excluir(7L, 3L);

		mvc.perform(delete("/entregas/3").with(usuario("7")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void idQueNaoEhNumeroDevolve400() throws Exception {
		mvc.perform(delete("/entregas/abc").with(usuario("7")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("REQUISICAO_INVALIDA"));
		verifyNoInteractions(entregaService);
	}

	@Test
	void excluirSemTokenDevolve401() throws Exception {
		mvc.perform(delete("/entregas/3")).andExpect(status().isUnauthorized());
		verifyNoInteractions(entregaService);
	}

}
