package com.ensora.auth;

import com.ensora.auth.dto.ConfiguracaoCadastroResponse;
import com.ensora.auth.dto.RegistrarRequest;
import com.ensora.auth.dto.UsuarioResponse;
import com.ensora.compartilhado.erro.AcessoNegadoException;
import com.ensora.compartilhado.erro.ConflitoException;
import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.NaoAutenticadoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
		when(authService.registrar(any(), any())).thenReturn(new UsuarioResponse(1L, "a@b.com"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\"}"))
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
						.content("{\"email\": \"nao-e-email\", \"senha\": \"senha-segura-1\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("email"));
	}

	@Test
	void registrarComCadastroDesabilitadoDevolve403() throws Exception {
		when(authService.registrar(any(), any())).thenThrow(new AcessoNegadoException("Cadastro desabilitado"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\"}"))
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
	void refreshComCookieDevolveNovoAccessTokenENovoCookie() throws Exception {
		SessaoCriada sessao = new SessaoCriada(new TokenAcesso("jwt-novo", 900),
				new RefreshTokenEmitido("refresh-novo", Duration.ofDays(30)));
		when(authService.refresh("refresh-antigo")).thenReturn(sessao);

		mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", "refresh-antigo")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenAcesso").value("jwt-novo"))
				.andExpect(header().string("Set-Cookie", containsString("refresh_token=refresh-novo")))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/auth")));
	}

	@Test
	void refreshSemCookieDevolve401() throws Exception {
		when(authService.refresh(isNull())).thenThrow(new NaoAutenticadoException("Sessão inválida"));

		mvc.perform(post("/auth/refresh"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("NAO_AUTENTICADO"))
				.andExpect(jsonPath("$.mensagem").value("Sessão inválida"));
	}

	@Test
	void refreshComSessaoInvalidaDevolve401SemCookieNovo() throws Exception {
		when(authService.refresh("revogado")).thenThrow(new NaoAutenticadoException("Sessão inválida"));

		mvc.perform(post("/auth/refresh").cookie(new Cookie("refresh_token", "revogado")))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	void logoutDevolve204ELimpaOCookieComOsMesmosAtributos() throws Exception {
		mvc.perform(post("/auth/logout").cookie(new Cookie("refresh_token", "atual")))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
				.andExpect(header().string("Set-Cookie", containsString("Path=/auth")))
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
				.andExpect(header().string("Set-Cookie", containsString("Secure")))
				.andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")));
		verify(authService).logout("atual");
	}

	@Test
	void logoutSemCookieTambemDevolve204() throws Exception {
		mvc.perform(post("/auth/logout"))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
		verify(authService).logout(isNull());
	}

	@Test
	void registrarComEmailRepetidoDevolve409() throws Exception {
		when(authService.registrar(any(), any())).thenThrow(new ConflitoException("E-mail já cadastrado"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.erro").value("CONFLITO"));
	}

	@Test
	void registrarComSenhaDeSeteCaracteresDevolve400() throws Exception {
		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"1234567\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("senha"));
	}

	@Test
	void registrarComSenhaDeOitoCaracteresDevolve201() throws Exception {
		when(authService.registrar(any(), any())).thenReturn(new UsuarioResponse(1L, "a@b.com"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"12345678\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void registrarRepassaOCodigoDeConviteEOIpAoService() throws Exception {
		when(authService.registrar(any(), any())).thenReturn(new UsuarioResponse(1L, "a@b.com"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\", \"codigoConvite\": \"familia\"}"))
				.andExpect(status().isCreated());

		ArgumentCaptor<RegistrarRequest> capturado = ArgumentCaptor.forClass(RegistrarRequest.class);
		verify(authService).registrar(capturado.capture(), eq("127.0.0.1"));
		assertThat(capturado.getValue().codigoConvite()).isEqualTo("familia");
	}

	@Test
	void registrarComCodigoDeConviteInvalidoDevolve400NoCampoCodigoConvite() throws Exception {
		when(authService.registrar(any(), any()))
				.thenThrow(new ValidacaoException("codigoConvite", "Código de convite inválido"));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\", \"codigoConvite\": \"errado\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("codigoConvite"));
	}

	@Test
	void registrarComMuitasTentativasDevolve429() throws Exception {
		when(authService.registrar(any(), any()))
				.thenThrow(new LimiteExcedidoException("Muitas tentativas de cadastro. Tente novamente mais tarde."));

		mvc.perform(post("/auth/registrar")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\": \"a@b.com\", \"senha\": \"senha-segura-1\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void configuracaoDeCadastroEPublicaEDevolveHabilitadoEExigeConvite() throws Exception {
		when(authService.configuracaoDeCadastro()).thenReturn(new ConfiguracaoCadastroResponse(true, true));

		mvc.perform(get("/auth/registro"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habilitado").value(true))
				.andExpect(jsonPath("$.exigeConvite").value(true));
	}

}
