package com.ensora.compartilhado.seguranca;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class ConfiguracaoSeguranca {

	@Bean
	PasswordEncoder codificadorDeSenha() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http,
			CorsConfigurationSource corsConfigurationSource,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedorDeErros,
			@Value("${ensora.api.prefixo:}") String prefixo) throws Exception {
		// 401 e 403 são entregues ao ManipuladorDeErros, para o formato de erro ter uma única fonte.
		AuthenticationEntryPoint naoAutenticado = (requisicao, resposta, ex) ->
				resolvedorDeErros.resolveException(requisicao, resposta, null, ex);
		AccessDeniedHandler acessoNegado = (requisicao, resposta, ex) ->
				resolvedorDeErros.resolveException(requisicao, resposta, null, ex);

		http
				// API stateless: o access token vai no header Authorization, não em cookie de sessão.
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.authorizeHttpRequests(autorizacao -> {
					autorizacao.requestMatchers(prefixo + "/auth/**").permitAll();
					if (prefixo.isEmpty()) {
						autorizacao.anyRequest().authenticated();
					} else {
						// Com prefixo, só a API exige token; a SPA e os arquivos estáticos são públicos.
						autorizacao.requestMatchers(prefixo + "/**").authenticated();
						autorizacao.anyRequest().permitAll();
					}
				})
				// Valida o JWT do header Authorization: Bearer (assinatura, expiração e emissor).
				.oauth2ResourceServer(oauth -> oauth
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(naoAutenticado)
						.accessDeniedHandler(acessoNegado))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(naoAutenticado)
						.accessDeniedHandler(acessoNegado));
		return http.build();
	}

}
