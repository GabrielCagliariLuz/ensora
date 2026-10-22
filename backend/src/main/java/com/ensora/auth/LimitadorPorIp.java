package com.ensora.auth;

import com.ensora.compartilhado.erro.LimiteExcedidoException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de ocorrências por IP numa janela de tempo, guardado em memória (zera se a API reiniciar). Cada uso
 * decide o que conta como ocorrência (falha de login, tentativa de cadastro) e o texto do bloqueio.
 */
public class LimitadorPorIp {

	private final Map<String, Deque<Instant>> ocorrenciasPorIp = new ConcurrentHashMap<>();
	private final Clock relogio;
	private final int maximo;
	private final Duration janela;
	private final String mensagemDeBloqueio;

	protected LimitadorPorIp(Clock relogio, int maximo, long janelaEmMinutos, String mensagemDeBloqueio) {
		this.relogio = relogio;
		this.maximo = maximo;
		this.janela = Duration.ofMinutes(janelaEmMinutos);
		this.mensagemDeBloqueio = mensagemDeBloqueio;
	}

	public void verificar(String ip) {
		if (contarRecentes(ip) >= maximo) {
			throw new LimiteExcedidoException(mensagemDeBloqueio);
		}
	}

	protected void registrar(String ip) {
		ocorrenciasPorIp.compute(ip, (chave, ocorrencias) -> {
			Deque<Instant> atuais = ocorrencias == null ? new ArrayDeque<>() : ocorrencias;
			descartarVencidas(atuais);
			atuais.addLast(relogio.instant());
			return atuais;
		});
	}

	public void limpar(String ip) {
		ocorrenciasPorIp.remove(ip);
	}

	private int contarRecentes(String ip) {
		int[] total = { 0 };
		ocorrenciasPorIp.computeIfPresent(ip, (chave, ocorrencias) -> {
			descartarVencidas(ocorrencias);
			total[0] = ocorrencias.size();
			return ocorrencias.isEmpty() ? null : ocorrencias;
		});
		return total[0];
	}

	private void descartarVencidas(Deque<Instant> ocorrencias) {
		Instant limite = relogio.instant().minus(janela);
		while (!ocorrencias.isEmpty() && ocorrencias.peekFirst().isBefore(limite)) {
			ocorrencias.removeFirst();
		}
	}

}
