package com.ensora.compartilhado.data;

import com.ensora.compartilhado.erro.ValidacaoException;

import java.time.ZoneId;

/** Interpretação do fuso horário que uma pessoa informa (cadastro e configurações). */
public final class FusoHorario {

	private FusoHorario() {
	}

	/**
	 * Aceita só nomes de região IANA ({@code America/Sao_Paulo}). Deslocamentos soltos ({@code +03:00}) ficam de
	 * fora: não seguem o horário de verão de lugar nenhum e dariam o dia errado parte do ano.
	 */
	public static ZoneId interpretar(String texto) {
		String nome = texto == null ? "" : texto.trim();
		if (!ZoneId.getAvailableZoneIds().contains(nome)) {
			throw new ValidacaoException("fuso", "fuso horário inválido");
		}
		return ZoneId.of(nome);
	}

}
