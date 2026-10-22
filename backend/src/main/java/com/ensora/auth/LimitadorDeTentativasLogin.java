package com.ensora.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Limite simples de falhas de login por IP, guardado em memória (zera se a API reiniciar). */
@Component
public class LimitadorDeTentativasLogin extends LimitadorPorIp {

	public LimitadorDeTentativasLogin(Clock relogio,
			@Value("${ensora.auth.login-max-tentativas}") int maximoDeFalhas,
			@Value("${ensora.auth.login-janela-minutos}") long janelaEmMinutos) {
		super(relogio, maximoDeFalhas, janelaEmMinutos, "Muitas tentativas de login. Tente novamente mais tarde.");
	}

	public void registrarFalha(String ip) {
		registrar(ip);
	}

}
