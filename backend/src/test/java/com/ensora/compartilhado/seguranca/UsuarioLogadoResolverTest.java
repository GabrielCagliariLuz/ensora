package com.ensora.compartilhado.seguranca;

import com.ensora.compartilhado.erro.NaoAutenticadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioLogadoResolverTest {

	private final UsuarioLogadoResolver resolvedor = new UsuarioLogadoResolver();

	@AfterEach
	void limparContextoDeSeguranca() {
		SecurityContextHolder.clearContext();
	}

	/** Métodos só para o teste ter parâmetros reais (com e sem a anotação) para perguntar ao resolvedor. */
	@SuppressWarnings("unused")
	static class Alvo {
		void comAnotacao(@UsuarioLogado Long usuarioId) {
		}

		void semAnotacao(Long usuarioId) {
		}

		void anotacaoEmTipoErrado(@UsuarioLogado String usuarioId) {
		}
	}

	private MethodParameter parametro(String metodo, Class<?> tipo) throws NoSuchMethodException {
		Method method = Alvo.class.getDeclaredMethod(metodo, tipo);
		return new MethodParameter(method, 0);
	}

	private void autenticarComoUsuario(String subject) {
		Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(900),
				java.util.Map.of("alg", "HS256"), java.util.Map.of("sub", subject));
		// O construtor só com o Jwt cria um token NÃO autenticado; o do Spring em produção traz as permissões (aqui, nenhuma).
		SecurityContextHolder.getContext()
				.setAuthentication(new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));
	}

	@Test
	void aceitaSoParametroLongComAAnotacao() throws Exception {
		assertThat(resolvedor.supportsParameter(parametro("comAnotacao", Long.class))).isTrue();
		assertThat(resolvedor.supportsParameter(parametro("semAnotacao", Long.class))).isFalse();
		assertThat(resolvedor.supportsParameter(parametro("anotacaoEmTipoErrado", String.class))).isFalse();
	}

	@Test
	void devolveOSubjectDoTokenComoLong() throws Exception {
		autenticarComoUsuario("7");

		Object id = resolvedor.resolveArgument(parametro("comAnotacao", Long.class), null, null, null);

		assertThat(id).isEqualTo(7L);
	}

	@Test
	void semAutenticacaoLancaNaoAutenticado() {
		assertThatThrownBy(() -> resolvedor.resolveArgument(null, null, null, null))
				.isInstanceOf(NaoAutenticadoException.class);
	}

	@Test
	void autenticacaoAnonimaLancaNaoAutenticado() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("chave",
				"anonimo", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

		assertThatThrownBy(() -> resolvedor.resolveArgument(null, null, null, null))
				.isInstanceOf(NaoAutenticadoException.class);
	}

	@Test
	void subjectQueNaoEhNumeroLancaNaoAutenticado() {
		autenticarComoUsuario("abc");

		assertThatThrownBy(() -> resolvedor.resolveArgument(null, null, null, null))
				.isInstanceOf(NaoAutenticadoException.class);
	}

}
