package com.ensora.auth;

import com.ensora.compartilhado.erro.LimiteExcedidoException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorDeCadastrosTest {

	private static final String IP = "203.0.113.7";

	/** Relógio que anda só quando o teste manda, para simular a passagem do tempo. */
	private static class RelogioManual extends Clock {
		private Instant agora = Instant.parse("2026-10-17T12:00:00Z");

		void avancar(Duration tempo) {
			agora = agora.plus(tempo);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zona) {
			return this;
		}

		@Override
		public Instant instant() {
			return agora;
		}
	}

	private final RelogioManual relogio = new RelogioManual();
	private final LimitadorDeCadastros limitador = new LimitadorDeCadastros(relogio, 10, 60);

	private void tentar(int vezes) {
		for (int i = 0; i < vezes; i++) {
			limitador.registrarTentativa(IP);
		}
	}

	@Test
	void novePrimeirasTentativasAindaNaoBloqueiam() {
		tentar(9);

		assertThatCode(() -> limitador.verificar(IP)).doesNotThrowAnyException();
	}

	@Test
	void dezTentativasNaJanelaBloqueiamComAMensagemDeCadastro() {
		tentar(10);

		assertThatThrownBy(() -> limitador.verificar(IP))
				.isInstanceOf(LimiteExcedidoException.class)
				.hasMessage("Muitas tentativas de cadastro. Tente novamente mais tarde.");
	}

	@Test
	void depoisDaJanelaAsTentativasAntigasSaemDaConta() {
		tentar(10);
		relogio.avancar(Duration.ofMinutes(61));

		assertThatCode(() -> limitador.verificar(IP)).doesNotThrowAnyException();
	}

	@Test
	void umIpBloqueadoNaoAfetaOutro() {
		tentar(10);

		assertThatCode(() -> limitador.verificar("198.51.100.9")).doesNotThrowAnyException();
	}

}
