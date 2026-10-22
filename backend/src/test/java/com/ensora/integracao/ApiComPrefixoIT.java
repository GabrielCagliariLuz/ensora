package com.ensora.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Produção: API sob /api, SPA na raiz e os dois na mesma origem. Contexto próprio, porque o prefixo muda o
 * mapeamento de todas as rotas.
 */
@SpringBootTest(properties = {
		"ensora.cors.origem=http://localhost:8080",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes",
		"ensora.auth.registro-habilitado=true",
		"ensora.auth.cadastro-max-tentativas=1000",
		"ensora.api.prefixo=/api"
})
@AutoConfigureMockMvc
class ApiComPrefixoIT {

	private static final String SENHA = "senha-de-teste-123";

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static {
		POSTGRES.start();
	}

	@Autowired
	private MockMvc mvc;

	@Test
	void autenticaSobOPrefixoEOCookieFicaNoCaminhoDaApi() throws Exception {
		String email = "u" + UUID.randomUUID() + "@teste.com";
		String corpo = "{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";

		mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());
		MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isOk())
				.andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")))
				.andReturn();
		assertThat(login.getResponse().getContentAsString()).contains("tokenAcesso");
	}

	@Test
	void rotaDaSpaDevolveOIndiceParaOReactResolver() throws Exception {
		mvc.perform(get("/calendario").param("dia", "2026-10-25"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("SPA de teste")));
	}

	@Test
	void apiSemTokenDa401EComTokenUmCaminhoDesconhecidoDa404() throws Exception {
		mvc.perform(get("/api/habitos")).andExpect(status().isUnauthorized());

		mvc.perform(get("/api/inexistente").with(jwt().jwt(token -> token.subject("1"))))
				.andExpect(status().isNotFound());
	}

	@Test
	void caminhoDeArquivoAusenteDa404EmVezDoIndice() throws Exception {
		mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
	}
}
