package com.ensora.compartilhado.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class ConfiguracaoCors {

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${ensora.cors.origem}") String origem) {
		CorsConfiguration configuracao = new CorsConfiguration();
		// Origem fixa: com allowCredentials o navegador não aceita "*".
		configuracao.setAllowedOrigins(List.of(origem));
		configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuracao.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/**", configuracao);
		return fonte;
	}

}
