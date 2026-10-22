package com.ensora.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Limite de tentativas de cadastro por IP, guardado em memória (zera se a API reiniciar). Conta toda tentativa, certa
 * ou errada: o limite protege a criação de contas em massa e também o palpite do código de convite.
 */
@Component
public class LimitadorDeCadastros extends LimitadorPorIp {

	public LimitadorDeCadastros(Clock relogio,
			@Value("${ensora.auth.cadastro-max-tentativas}") int maximoDeTentativas,
			@Value("${ensora.auth.cadastro-janela-minutos}") long janelaEmMinutos) {
		super(relogio, maximoDeTentativas, janelaEmMinutos,
				"Muitas tentativas de cadastro. Tente novamente mais tarde.");
	}

	public void registrarTentativa(String ip) {
		registrar(ip);
	}

}
