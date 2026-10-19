package com.ensora.compartilhado;

import com.ensora.auth.TokenAcessoService;
import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @WebMvcTest só carrega controllers, advices e filtros: as configurações de segurança e CORS precisam ser importadas.
@WebMvcTest(ControladorDeTesteSeguranca.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class,
		TokenAcessoService.class, ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes",
		"ensora.jwt.expiracao-minutos=15"
})
class SegurancaEErrosTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private TokenAcessoService tokenAcessoService;

	@Autowired
	private JwtEncoder codificador;

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

	@Test
	void regraDeNegocioViolada_Devolve400ComOCampoEAMensagem() throws Exception {
		mvc.perform(get("/habitos/teste-regra-violada").with(SecurityMockMvcRequestPostProcessors.user("usuario")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("vezesPorSemana"))
				.andExpect(jsonPath("$.campos[0].mensagem").value("obrigatória para hábito semanal"));
	}

	@Test
	void tokenValidoLiberaRotaProtegida() throws Exception {
		String token = tokenAcessoService.gerar(1L).valor();

		// 404 (e não 401) mostra que a requisição passou pela segurança e chegou ao controller.
		mvc.perform(get("/habitos/teste-nao-encontrado").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("NAO_ENCONTRADO"));
	}

	@Test
	void tokenComAssinaturaAdulteradaDevolve401() throws Exception {
		String valido = tokenAcessoService.gerar(1L).valor();
		String adulterado = valido.substring(0, valido.lastIndexOf('.') + 1) + "assinaturafalsa";

		mvc.perform(get("/habitos/teste-nao-encontrado").header("Authorization", "Bearer " + adulterado))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
	}

	@Test
	void tokenExpiradoDevolve401() throws Exception {
		Instant agora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ConfiguracaoJwt.EMISSOR)
				.subject("1")
				.issuedAt(agora.minus(Duration.ofHours(2)))
				.expiresAt(agora.minus(Duration.ofHours(1)))
				.build();
		String expirado = codificador
				.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();

		mvc.perform(get("/habitos/teste-nao-encontrado").header("Authorization", "Bearer " + expirado))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"));
	}

	private MockHttpServletRequestBuilder preflight(String origem) {
		return options("/habitos")
				.header("Origin", origem)
				.header("Access-Control-Request-Method", "GET");
	}

}
