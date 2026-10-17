package com.ensora.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

	private static final int TAMANHO_DO_TOKEN_EM_BYTES = 32;

	private final RefreshTokenRepository refreshTokens;
	private final Clock relogio;
	private final Duration validade;
	private final SecureRandom aleatorio = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository refreshTokens, Clock relogio,
			@Value("${ensora.refresh.expiracao-dias}") long expiracaoEmDias) {
		this.refreshTokens = refreshTokens;
		this.relogio = relogio;
		this.validade = Duration.ofDays(expiracaoEmDias);
	}

	/** Cria um token opaco, guarda só o hash dele e devolve o valor em claro (esta é a única vez que ele existe). */
	public RefreshTokenEmitido criar(Long usuarioId) {
		byte[] bytes = new byte[TAMANHO_DO_TOKEN_EM_BYTES];
		aleatorio.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		RefreshToken registro = new RefreshToken();
		registro.setUsuarioId(usuarioId);
		registro.setTokenHash(hash(token));
		registro.setExpiraEm(relogio.instant().plus(validade));
		refreshTokens.save(registro);

		return new RefreshTokenEmitido(token, validade);
	}

	/**
	 * SHA-256 em hexadecimal. É um hash rápido de propósito: o token já é aleatório e longo, e a busca
	 * por token precisa achar a linha pelo hash (o que o BCrypt, que usa sal, não permite).
	 */
	static String hash(String token) {
		try {
			byte[] resumo = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(resumo);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 indisponível", e);
		}
	}

}
