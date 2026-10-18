package com.ensora.auth;

import com.ensora.compartilhado.erro.NaoAutenticadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private static final Instant AGORA = Instant.parse("2026-10-17T12:00:00Z");
	private static final String TOKEN_ANTIGO = "token-antigo-em-claro";

	@Mock
	private RefreshTokenRepository refreshTokens;

	private RefreshTokenService servico() {
		return new RefreshTokenService(refreshTokens, Clock.fixed(AGORA, ZoneOffset.UTC), 30);
	}

	private RefreshToken registro(Instant expiraEm, Instant revogadoEm) {
		RefreshToken registro = new RefreshToken();
		registro.setId(1L);
		registro.setUsuarioId(7L);
		registro.setTokenHash(RefreshTokenService.hash(TOKEN_ANTIGO));
		registro.setExpiraEm(expiraEm);
		registro.setRevogadoEm(revogadoEm);
		return registro;
	}

	private void encontrar(RefreshToken registro) {
		when(refreshTokens.findByTokenHash(RefreshTokenService.hash(TOKEN_ANTIGO))).thenReturn(Optional.of(registro));
	}

	@Test
	void criarGuardaApenasOHashEAValidadeDeTrintaDias() {
		when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		RefreshTokenEmitido emitido = servico().criar(7L);

		ArgumentCaptor<RefreshToken> salvo = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(salvo.capture());
		assertThat(salvo.getValue().getUsuarioId()).isEqualTo(7L);
		assertThat(salvo.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(emitido.valor()));
		assertThat(salvo.getValue().getTokenHash()).isNotEqualTo(emitido.valor());
		assertThat(salvo.getValue().getExpiraEm()).isEqualTo(AGORA.plus(Duration.ofDays(30)));
		assertThat(emitido.validade()).isEqualTo(Duration.ofDays(30));
		// 32 bytes em base64url sem preenchimento têm 43 caracteres.
		assertThat(emitido.valor()).hasSize(43);
	}

	@Test
	void rotacionarComTokenValidoRevogaOAntigoECriaUmNovo() {
		encontrar(registro(AGORA.plus(Duration.ofDays(10)), null));
		when(refreshTokens.revogarSeAtivo(1L, AGORA)).thenReturn(1);
		when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		RotacaoRefresh rotacao = servico().rotacionar(TOKEN_ANTIGO);

		assertThat(rotacao.usuarioId()).isEqualTo(7L);
		assertThat(rotacao.novoRefreshToken().valor()).isNotEqualTo(TOKEN_ANTIGO);
		verify(refreshTokens).revogarSeAtivo(1L, AGORA);
		ArgumentCaptor<RefreshToken> novo = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(novo.capture());
		assertThat(novo.getValue().getTokenHash())
				.isEqualTo(RefreshTokenService.hash(rotacao.novoRefreshToken().valor()));
		verify(refreshTokens, never()).revogarTodosDoUsuario(any(), any());
	}

	@Test
	void rotacionarComTokenDesconhecidoLancaNaoAutenticado() {
		when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().rotacionar("qualquer-coisa"))
				.isInstanceOf(NaoAutenticadoException.class)
				.hasMessage("Sessão inválida");
		verify(refreshTokens, never()).revogarSeAtivo(any(), any());
		verify(refreshTokens, never()).revogarTodosDoUsuario(any(), any());
	}

	@Test
	void rotacionarComTokenExpiradoLancaNaoAutenticadoSemRevogarAsOutrasSessoes() {
		encontrar(registro(AGORA.minusSeconds(1), null));

		assertThatThrownBy(() -> servico().rotacionar(TOKEN_ANTIGO))
				.isInstanceOf(NaoAutenticadoException.class);
		verify(refreshTokens, never()).revogarTodosDoUsuario(any(), any());
		verify(refreshTokens, never()).revogarSeAtivo(any(), any());
		verify(refreshTokens, never()).save(any());
	}

	@Test
	void reusoDeTokenJaRevogadoRevogaTodasAsSessoesDoUsuario() {
		encontrar(registro(AGORA.plus(Duration.ofDays(10)), AGORA.minus(Duration.ofHours(1))));

		assertThatThrownBy(() -> servico().rotacionar(TOKEN_ANTIGO))
				.isInstanceOf(NaoAutenticadoException.class)
				.hasMessage("Sessão inválida");
		verify(refreshTokens).revogarTodosDoUsuario(7L, AGORA);
		verify(refreshTokens, never()).save(any());
	}

	@Test
	void quandoOutraRequisicaoGirouOTokenPrimeiroTrataComoReuso() {
		encontrar(registro(AGORA.plus(Duration.ofDays(10)), null));
		// Zero linhas atualizadas: entre a leitura e o UPDATE, outra requisição já revogou este token.
		when(refreshTokens.revogarSeAtivo(1L, AGORA)).thenReturn(0);

		assertThatThrownBy(() -> servico().rotacionar(TOKEN_ANTIGO))
				.isInstanceOf(NaoAutenticadoException.class);
		verify(refreshTokens).revogarTodosDoUsuario(7L, AGORA);
		verify(refreshTokens, never()).save(any());
	}

	@Test
	void revogarMarcaOTokenAtivoComoRevogado() {
		encontrar(registro(AGORA.plus(Duration.ofDays(10)), null));

		servico().revogar(TOKEN_ANTIGO);

		verify(refreshTokens).revogarSeAtivo(1L, AGORA);
	}

	@Test
	void revogarTokenDesconhecidoNaoFalhaNemMexeEmNada() {
		when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.empty());

		assertThatCode(() -> servico().revogar("desconhecido")).doesNotThrowAnyException();
		verify(refreshTokens, never()).revogarSeAtivo(any(), any());
	}

}
