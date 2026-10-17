package com.ensora.compartilhado.erro;

import org.springframework.security.core.AuthenticationException;

public class NaoAutenticadoException extends AuthenticationException {

	public NaoAutenticadoException(String mensagem) {
		super(mensagem);
	}

}
