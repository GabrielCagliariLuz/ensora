package com.ensora.compartilhado;

import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest só carrega controllers, advices e filtros: as configurações de segurança e CORS precisam ser importadas.
@WebMvcTest(ControladorDeTesteSeguranca.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ManipuladorDeErros.class })
@TestPropertySource(properties = "ensora.cors.origem=http://localhost:5173")
class SegurancaEErrosTest {

	@Autowired
	private MockMvc mvc;

	@Test
	void rotaProtegidaSemTokenDevolve401NoFormatoDeErro() throws Exception {
		mvc.perform(get("/habitos/teste-acesso-negado"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"))
				.andExpect(jsonPath("$.caminho").value("/habitos/teste-acesso-negado"));
	}

	@Test
	void rotaAuthEhPublicaMesmoSemToken() throws Exception {
		mvc.perform(get("/auth/teste"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void corpoInvalidoDevolve400ComListaDeCampos() throws Exception {
		mvc.perform(post("/habitos/teste-validacao")
						.with(SecurityMockMvcRequestPostProcessors.user("usuario"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nome\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("nome"));
	}

	@Test
	void acessoNegadoDevolve403ComFormatoDeErro() throws Exception {
		mvc.perform(get("/habitos/teste-acesso-negado").with(SecurityMockMvcRequestPostProcessors.user("usuario")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"));
	}

	@Test
	void recursoInexistenteDevolve404ComFormatoDeErro() throws Exception {
		mvc.perform(get("/habitos/teste-nao-encontrado").with(SecurityMockMvcRequestPostProcessors.user("usuario")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"))
				.andExpect(jsonPath("$.mensagem").value("Hábito não encontrado"));
	}

	@Test
	void preflightDeOrigemPermitidaDevolveCabecalhosDeCors() throws Exception {
		mvc.perform(preflight("http://localhost:5173"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(header().string("Access-Control-Allow-Credentials", "true"));
	}

	@Test
	void preflightDeOrigemDesconhecidaNaoDevolveCabecalhoDeOrigem() throws Exception {
		mvc.perform(preflight("http://site-estranho.com"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	private MockHttpServletRequestBuilder preflight(String origem) {
		return options("/habitos")
				.header("Origin", origem)
				.header("Access-Control-Request-Method", "GET");
	}

}
