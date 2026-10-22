package com.ensora.integracao;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de integração: app inteira (filtros, JWT, controllers, services, JPQL) contra um PostgreSQL de
 * verdade, num container descartável. Rodam só no {@code ./mvnw verify} e exigem o Docker aberto.
 *
 * <p>O container é um só para todas as classes (sobe na primeira e morre com a JVM), e as contas de cada teste são
 * novas, com e-mail único: nenhum teste depende de limpeza do banco nem de ordem.
 */
@SpringBootTest(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes",
		"ensora.auth.registro-habilitado=true",
		// Todos os cadastros vêm do mesmo IP (o do MockMvc): o limite padrão de 10 por hora barraria a suíte.
		"ensora.auth.cadastro-max-tentativas=1000"
})
@AutoConfigureMockMvc
abstract class IntegracaoBase {

	protected static final String SENHA = "senha-de-teste-123";

	@ServiceConnection
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

	static {
		POSTGRES.start();
	}

	@Autowired
	protected MockMvc mvc;

	/** Quem faz as requisições: o id da conta e o token de acesso dela. */
	protected record Sessao(long id, String email, String token) {
	}

	protected Sessao novaConta() throws Exception {
		String email = "u" + UUID.randomUUID() + "@teste.com";
		MvcResult cadastro = mvc.perform(post("/auth/registrar").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		long id = ((Number) JsonPath.read(cadastro.getResponse().getContentAsString(), "$.id")).longValue();

		MvcResult login = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk()).andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.tokenAcesso");
		return new Sessao(id, email, token);
	}

	protected MockHttpServletRequestBuilder como(Sessao sessao, MockHttpServletRequestBuilder requisicao) {
		return requisicao.header("Authorization", "Bearer " + sessao.token());
	}

	protected MockHttpServletRequestBuilder comJson(MockHttpServletRequestBuilder requisicao, String corpo) {
		return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	/** Cria o recurso na conta informada e devolve o id. */
	protected long criar(Sessao dono, String caminho, String corpo) throws Exception {
		MvcResult resultado = mvc.perform(comJson(como(dono, post(caminho)), corpo))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(resultado.getResponse().getContentAsString(), "$.id")).longValue();
	}

	protected String ler(Sessao sessao, String caminho) throws Exception {
		return mvc.perform(como(sessao, get(caminho))).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
	}
}
