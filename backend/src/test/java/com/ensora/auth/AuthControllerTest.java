package com.ensora.auth;

import com.ensora.auth.dto.UsuarioResponse;
import com.ensora.compartilhado.erro.AcessoNegadoException;
import com.ensora.compartilhado.erro.ConflitoException;
import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.NaoAutenticadoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class AuthControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void registrarDevolve201ComIdEEmail() throws Exception {
		when(authService.registrar(any())).thenReturn(new UsuarioResponse(1L, "a@b.com"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.email").value("a@b.com"));
	}

	@Test
	void registrarComSenhaDeTresCaracteresDevolve400() throws Exception {
		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("senha"));
	}

	@Test
	void registrarComEmailInvalidoDevolve400() throws Exception {
		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"nao-e-email\", \"senha\": \"1234\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("email"));
	}

	@Test
	void registrarComCadastroDesabilitadoDevolve403() throws Exception {
		when(authService.registrar(any())).thenThrow(new AcessoNegadoException("Cadastro desabilitado"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.erro").value("ACESSO_NEGADO"))
				.andExpect(jsonPath("$.mensagem").value("Cadastro desabilitado"));
	}

	@Test
	void loginDevolveTokenNoCorpoERefreshTokenNoCookieSeguro() throws Exception {
		SessaoCriada sessao = new SessaoCriada(new TokenAcesso("jwt-de-teste", 900),
				new RefreshTokenEmitido("refresh-de-teste", Duration.ofDays(30)));
		when(authService.login(any(), any())).thenReturn(sessao);

		mvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenAcesso").value("jwt-de-teste"))
				.andExpect(jsonPath("$.expiraEmSegundos").value(900))
				.andExpect(header().string("Set-Cookie", containsString("refresh_token=refresh-de-teste")))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/auth")))
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=2592000")));
	}

	@Test
	void loginComCredenciaisInvalidasDevolve401Generico() throws Exception {
		when(authService.login(any(), any())).thenThrow(new NaoAutenticadoException("E-mail ou senha inválidos"));

		mvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"errada\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"))
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"))
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	void loginComLimiteExcedidoDevolve429() throws Exception {
		when(authService.login(any(), any())).thenThrow(new LimiteExcedidoException("Muitas tentativas"));

		mvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234\"}"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.erro").value("LIMITE_EXCEDIDO"));
	}

	@Test
	void loginSemSenhaDevolve400() throws Exception {
		mvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("senha"));
	}

	@Test
	void registrarComEmailRepetidoDevolve409() throws Exception {
		when(authService.registrar(any())).thenThrow(new ConflitoException("E-mail já cadastrado"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.erro").value("CONFLITO"));
	}

}
