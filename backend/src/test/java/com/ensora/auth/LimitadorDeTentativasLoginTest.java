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

class LimitadorDeTentativasLoginTest {

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
	private final LimitadorDeTentativasLogin limitador = new LimitadorDeTentativasLogin(relogio, 5, 15);

	private void falhar(int vezes) {
		for (int i = 0; i < vezes; i++) {
			limitador.registrarFalha(IP);
		}
	}

	@Test
	void quatroFalhasAindaNaoBloqueiam() {
		falhar(4);

		assertThatCode(() -> limitador.verificar(IP)).doesNotThrowAnyException();
	}

	@Test
	void cincoFalhasNaJanelaBloqueiam() {
		falhar(5);

		assertThatThrownBy(() -> limitador.verificar(IP)).isInstanceOf(LimiteExcedidoException.class);
	}

	@Test
	void depoisDaJanelaAsFalhasAntigasSaemDaConta() {
		falhar(5);
		relogio.avancar(Duration.ofMinutes(16));

		assertThatCode(() -> limitador.verificar(IP)).doesNotThrowAnyException();
	}

	@Test
	void limparZeraAsFalhasDoIp() {
		falhar(5);
		limitador.limpar(IP);

		assertThatCode(() -> limitador.verificar(IP)).doesNotThrowAnyException();
	}

}
