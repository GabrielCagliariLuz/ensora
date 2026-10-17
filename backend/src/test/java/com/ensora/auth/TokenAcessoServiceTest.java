package com.ensora.auth;

import com.ensora.compartilhado.seguranca.ConfiguracaoJwt;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenAcessoServiceTest {

	private static final String SEGREDO = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes";
	private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-tambem-com-mais-de-32-bytes";

	private final ConfiguracaoJwt configuracao = new ConfiguracaoJwt();
	private final SecretKey chave = configuracao.chaveJwt(SEGREDO);
	private final JwtEncoder codificador = configuracao.jwtEncoder(chave);
	private final JwtDecoder decodificador = configuracao.jwtDecoder(chave);

	@Test
	void tokenGeradoTrazSubjectEmissorEExpiracaoDeQuinzeMinutos() {
		TokenAcessoService servico = new TokenAcessoService(codificador, Clock.systemUTC(), 15);

		TokenAcesso token = servico.gerar(42L);
		Jwt jwt = decodificador.decode(token.valor());

		assertThat(jwt.getSubject()).isEqualTo("42");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("ensora");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
		assertThat(token.expiraEmSegundos()).isEqualTo(900);
	}

	@Test
	void tokenExpiradoEhRecusado() {
		Clock umaHoraAtras = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
		TokenAcessoService servico = new TokenAcessoService(codificador, umaHoraAtras, 15);

		TokenAcesso token = servico.gerar(42L);

		assertThatThrownBy(() -> decodificador.decode(token.valor())).isInstanceOf(JwtException.class);
	}

	@Test
	void tokenAssinadoComOutroSegredoEhRecusado() {
		ConfiguracaoJwt outraConfiguracao = new ConfiguracaoJwt();
		JwtEncoder codificadorDoAtacante = outraConfiguracao.jwtEncoder(outraConfiguracao.chaveJwt(OUTRO_SEGREDO));
		TokenAcessoService servicoDoAtacante = new TokenAcessoService(codificadorDoAtacante, Clock.systemUTC(), 15);

		TokenAcesso falso = servicoDoAtacante.gerar(42L);

		assertThatThrownBy(() -> decodificador.decode(falso.valor())).isInstanceOf(JwtException.class);
	}

	@Test
	void segredoComMenosDe32BytesFalhaNaSubida() {
		assertThatThrownBy(() -> configuracao.chaveJwt("curto"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32 bytes");
	}

}
