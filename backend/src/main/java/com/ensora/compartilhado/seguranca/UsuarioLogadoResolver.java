package com.ensora.compartilhado.seguranca;

import com.ensora.compartilhado.erro.NaoAutenticadoException;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Entrega ao Controller o id do usuário: o {@code sub} do JWT, que é o id da tabela usuario. */
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

	private static final String MENSAGEM = "Autenticação necessária";

	@Override
	public boolean supportsParameter(MethodParameter parametro) {
		return parametro.hasParameterAnnotation(UsuarioLogado.class)
				&& Long.class.equals(parametro.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer container,
			NativeWebRequest requisicao, WebDataBinderFactory fabrica) {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated()
				|| autenticacao instanceof AnonymousAuthenticationToken) {
			throw new NaoAutenticadoException(MENSAGEM);
		}
		try {
			// Em um JwtAuthenticationToken, getName() é o subject do token.
			return Long.valueOf(autenticacao.getName());
		} catch (NumberFormatException e) {
			throw new NaoAutenticadoException(MENSAGEM);
		}
	}

}
