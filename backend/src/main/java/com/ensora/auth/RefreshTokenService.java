package com.ensora.auth;

import com.ensora.compartilhado.erro.NaoAutenticadoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

	private static final int TAMANHO_DO_TOKEN_EM_BYTES = 32;
	private static final String MENSAGEM_SESSAO_INVALIDA = "Sessão inválida";

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
	 * Troca um refresh token por outro: o recebido é revogado e um novo é criado. Um token só vale uma vez;
	 * se um já usado aparecer de novo, todas as sessões do usuário são revogadas.
	 * <p>
	 * {@code noRollbackFor}: no caso de reuso o método grava (revoga tudo) e depois lança a exceção de 401.
	 * Sem isso, a exceção desfaria a transação e a revogação geral seria perdida.
	 */
	@Transactional(noRollbackFor = NaoAutenticadoException.class)
	public RotacaoRefresh rotacionar(String tokenEmClaro) {
		Instant agora = relogio.instant();
		RefreshToken atual = refreshTokens.findByTokenHash(hash(tokenEmClaro))
				.orElseThrow(() -> new NaoAutenticadoException(MENSAGEM_SESSAO_INVALIDA));

		if (atual.getRevogadoEm() != null) {
			throw revogarTudoPorReuso(atual.getUsuarioId(), agora);
		}
		if (!atual.getExpiraEm().isAfter(agora)) {
			throw new NaoAutenticadoException(MENSAGEM_SESSAO_INVALIDA);
		}
		// O UPDATE condicional decide no banco quem ganha se duas requisições chegarem com o mesmo token.
		if (refreshTokens.revogarSeAtivo(atual.getId(), agora) == 0) {
			throw revogarTudoPorReuso(atual.getUsuarioId(), agora);
		}

		return new RotacaoRefresh(atual.getUsuarioId(), criar(atual.getUsuarioId()));
	}

	/** Revoga o token desta sessão. Token desconhecido ou já revogado é ignorado (logout é idempotente). */
	@Transactional
	public void revogar(String tokenEmClaro) {
		refreshTokens.findByTokenHash(hash(tokenEmClaro))
				.ifPresent(token -> refreshTokens.revogarSeAtivo(token.getId(), relogio.instant()));
	}

	private NaoAutenticadoException revogarTudoPorReuso(Long usuarioId, Instant agora) {
		refreshTokens.revogarTodosDoUsuario(usuarioId, agora);
		return new NaoAutenticadoException(MENSAGEM_SESSAO_INVALIDA);
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

	/** Revoga todas as sessões do usuário (troca de senha). Quem estiver logado precisa entrar de novo. */
	@Transactional
	public void revogarTodas(Long usuarioId) {
		refreshTokens.revogarTodosDoUsuario(usuarioId, relogio.instant());
	}

	/** Apaga todas as sessões do usuário, para a conta poder ser excluída. */
	@Transactional
	public void apagarTodas(Long usuarioId) {
		refreshTokens.apagarDoUsuario(usuarioId);
	}

}
