package com.ensora.conta;

import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.ManipuladorDeErros;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.compartilhado.seguranca.ConfiguracaoCors;
import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import com.ensora.compartilhado.seguranca.ConfiguracaoSeguranca;
import com.ensora.compartilhado.seguranca.ConfiguracaoWeb;
import com.ensora.conta.dto.ContaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContaController.class)
@Import({ ConfiguracaoSeguranca.class, ConfiguracaoCors.class, ConfiguracaoJwt.class, ConfiguracaoWeb.class,
		ManipuladorDeErros.class })
@TestPropertySource(properties = {
		"ensora.cors.origem=http://localhost:5173",
		"ensora.jwt.segredo=segredo-de-teste-com-mais-de-trinta-e-dois-bytes"
})
class ContaControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ContaService contaService;

	private RequestPostProcessor usuario(String id) {
		return jwt().jwt(token -> token.subject(id));
	}

	@Test
	void sempreExigeAutenticacao() throws Exception {
		mvc.perform(get("/usuario")).andExpect(status().isUnauthorized());
		mvc.perform(put("/usuario/fuso").contentType(MediaType.APPLICATION_JSON).content("{\"fuso\": \"Asia/Tokyo\"}"))
				.andExpect(status().isUnauthorized());
		mvc.perform(put("/usuario/introducao")).andExpect(status().isUnauthorized());
		mvc.perform(delete("/usuario").contentType(MediaType.APPLICATION_JSON).content("{\"senha\": \"x\"}"))
				.andExpect(status().isUnauthorized());
		verifyNoInteractions(contaService);
	}

	@Test
	void buscarDevolve200ComIdEmailEFusoDoUsuarioDoToken() throws Exception {
		when(contaService.buscar(7L)).thenReturn(new ContaResponse(7L, "a@b.com", "America/Manaus", false));

		mvc.perform(get("/usuario").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.email").value("a@b.com"))
				.andExpect(jsonPath("$.fuso").value("America/Manaus"))
				.andExpect(jsonPath("$.introducaoConcluida").value(false));
	}

	@Test
	void concluirIntroducaoDevolve200ComAContaDoUsuarioDoToken() throws Exception {
		when(contaService.concluirIntroducao(7L)).thenReturn(new ContaResponse(7L, "a@b.com", "America/Manaus", true));

		mvc.perform(put("/usuario/introducao").with(usuario("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.introducaoConcluida").value(true));
	}

	@Test
	void alterarFusoDevolve200ComAContaAtualizada() throws Exception {
		when(contaService.alterarFuso(7L, "Asia/Tokyo")).thenReturn(new ContaResponse(7L, "a@b.com", "Asia/Tokyo", false));

		mvc.perform(put("/usuario/fuso").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"fuso\": \"Asia/Tokyo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fuso").value("Asia/Tokyo"));
	}

	@Test
	void alterarFusoEmBrancoDevolve400NoCampoFuso() throws Exception {
		mvc.perform(put("/usuario/fuso").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"fuso\": \"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("fuso"));
		verifyNoInteractions(contaService);
	}

	@Test
	void alterarFusoInvalidoDevolve400VindoDoService() throws Exception {
		when(contaService.alterarFuso(7L, "Marte/Olimpo")).thenThrow(new ValidacaoException("fuso", "fuso horário inválido"));

		mvc.perform(put("/usuario/fuso").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"fuso\": \"Marte/Olimpo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("fuso"));
	}

	@Test
	void alterarSenhaDevolve204EPassaAsDuasSenhasEOIp() throws Exception {
		mvc.perform(put("/usuario/senha").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\": \"senha-antiga\", \"novaSenha\": \"senha-nova-123\"}"))
				.andExpect(status().isNoContent());

		verify(contaService).alterarSenha(7L, "senha-antiga", "senha-nova-123", "127.0.0.1");
	}

	@Test
	void alterarSenhaComNovaSenhaDeSeteCaracteresDevolve400NoCampoNovaSenha() throws Exception {
		mvc.perform(put("/usuario/senha").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\": \"senha-antiga\", \"novaSenha\": \"1234567\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("novaSenha"));
		verifyNoInteractions(contaService);
	}

	@Test
	void alterarSenhaComSenhaAtualErradaDevolve400NoCampoSenhaAtual() throws Exception {
		doThrow(new ValidacaoException("senhaAtual", "senha atual incorreta"))
				.when(contaService).alterarSenha(any(), any(), any(), any());

		mvc.perform(put("/usuario/senha").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"senhaAtual\": \"errada\", \"novaSenha\": \"senha-nova-123\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("VALIDACAO"))
				.andExpect(jsonPath("$.campos[0].campo").value("senhaAtual"));
	}

	@Test
	void excluirDevolve204EPassaASenhaEOIp() throws Exception {
		mvc.perform(delete("/usuario").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"senha\": \"senha-segura\"}"))
				.andExpect(status().isNoContent());

		verify(contaService).excluir(7L, "senha-segura", "127.0.0.1");
	}

	@Test
	void excluirSemSenhaDevolve400() throws Exception {
		mvc.perform(delete("/usuario").with(usuario("7")).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos[0].campo").value("senha"));
		verifyNoInteractions(contaService);
	}

	@Test
	void excluirComMuitosPalpitesDevolve429() throws Exception {
		doThrow(new LimiteExcedidoException("Muitas tentativas de login. Tente novamente mais tarde."))
				.when(contaService).excluir(any(), any(), any());

		mvc.perform(delete("/usuario").with(usuario("7")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"senha\": \"palpite\"}"))
				.andExpect(status().isTooManyRequests());
	}

}
