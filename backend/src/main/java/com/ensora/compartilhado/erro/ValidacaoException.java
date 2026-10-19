package com.ensora.compartilhado.erro;

/** Regra de validação que o Bean Validation não expressa bem (ex.: "semanal exige meta"). Vira 400 VALIDACAO. */
public class ValidacaoException extends RuntimeException {

	private final String campo;

	public ValidacaoException(String campo, String mensagem) {
		super(mensagem);
		this.campo = campo;
	}

	public String getCampo() {
		return campo;
	}

}
