package com.ensora.auth;

import com.ensora.compartilhado.erro.LimiteExcedidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Limite simples de falhas de login por IP, guardado em memória (zera se a API reiniciar). */
@Component
public class LimitadorDeTentativasLogin {

	private final Map<String, Deque<Instant>> falhasPorIp = new ConcurrentHashMap<>();
	private final Clock relogio;
	private final int maximoDeFalhas;
	private final Duration janela;

	public LimitadorDeTentativasLogin(Clock relogio,
			@Value("${ensora.auth.login-max-tentativas}") int maximoDeFalhas,
			@Value("${ensora.auth.login-janela-minutos}") long janelaEmMinutos) {
		this.relogio = relogio;
		this.maximoDeFalhas = maximoDeFalhas;
		this.janela = Duration.ofMinutes(janelaEmMinutos);
	}

	public void verificar(String ip) {
		if (contarFalhasRecentes(ip) >= maximoDeFalhas) {
			throw new LimiteExcedidoException("Muitas tentativas de login. Tente novamente mais tarde.");
		}
	}

	public void registrarFalha(String ip) {
		falhasPorIp.compute(ip, (chave, falhas) -> {
			Deque<Instant> atuais = falhas == null ? new ArrayDeque<>() : falhas;
			descartarVencidas(atuais);
			atuais.addLast(relogio.instant());
			return atuais;
		});
	}

	public void limpar(String ip) {
		falhasPorIp.remove(ip);
	}

	private int contarFalhasRecentes(String ip) {
		int[] total = { 0 };
		falhasPorIp.computeIfPresent(ip, (chave, falhas) -> {
			descartarVencidas(falhas);
			total[0] = falhas.size();
			return falhas.isEmpty() ? null : falhas;
		});
		return total[0];
	}

	private void descartarVencidas(Deque<Instant> falhas) {
		Instant limite = relogio.instant().minus(janela);
		while (!falhas.isEmpty() && falhas.peekFirst().isBefore(limite)) {
			falhas.removeFirst();
		}
	}

}
