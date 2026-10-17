package com.ensora.compartilhado.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Configuration
public class ConfiguracaoJwt {

	public static final String EMISSOR = "ensora";

	private static final int TAMANHO_MINIMO_SEGREDO_EM_BYTES = 32;

	@Bean
	public Clock relogio() {
		return Clock.systemUTC();
	}

	@Bean
	public SecretKey chaveJwt(@Value("${ensora.jwt.segredo}") String segredo) {
		byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < TAMANHO_MINIMO_SEGREDO_EM_BYTES) {
			throw new IllegalStateException(
					"JWT_SEGREDO precisa ter ao menos " + TAMANHO_MINIMO_SEGREDO_EM_BYTES + " bytes");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey chaveJwt) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chaveJwt)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		// O validador padrão confere a expiração; este acrescenta a conferência do emissor.
		decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
		return decodificador;
	}

}
