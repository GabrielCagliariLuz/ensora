package com.ensora.conta.dto;

import com.ensora.auth.Usuario;

public record ContaResponse(Long id, String email, String fuso, boolean introducaoConcluida) {

	public static ContaResponse de(Usuario usuario) {
		return new ContaResponse(usuario.getId(), usuario.getEmail(), usuario.getFuso(),
				usuario.getIntroducaoConcluidaEm() != null);
	}

}
