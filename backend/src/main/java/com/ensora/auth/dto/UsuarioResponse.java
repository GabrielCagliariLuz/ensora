package com.ensora.auth.dto;

import com.ensora.auth.Usuario;

public record UsuarioResponse(Long id, String email) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getEmail());
	}

}
