package com.ensora.auth;

import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class TokenAcessoService {

	private final JwtEncoder codificador;
	private final Clock relogio;
	private final Duration validade;

	public TokenAcessoService(JwtEncoder codificador, Clock relogio,
			@Value("${ensora.jwt.expiracao-minutos}") long expiracaoEmMinutos) {
		this.codificador = codificador;
		this.relogio = relogio;
		this.validade = Duration.ofMinutes(expiracaoEmMinutos);
	}

	public TokenAcesso gerar(Long usuarioId) {
		Instant agora = relogio.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ConfiguracaoJwt.EMISSOR)
				.subject(String.valueOf(usuarioId))
				.issuedAt(agora)
				.expiresAt(agora.plus(validade))
				.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
		return new TokenAcesso(valor, validade.toSeconds());
	}

}
