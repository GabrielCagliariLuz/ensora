package com.ensora.compartilhado.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResposta(
		int status,
		String erro,
		String mensagem,
		String caminho,
		List<ErroCampo> campos) {
}
