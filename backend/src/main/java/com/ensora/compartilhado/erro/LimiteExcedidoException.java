package com.ensora.compartilhado.erro;

public class LimiteExcedidoException extends RuntimeException {

	public LimiteExcedidoException(String mensagem) {
		super(mensagem);
	}

}
