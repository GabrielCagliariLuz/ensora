package com.ensora.auth;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

/** Dados da conta que outros módulos precisam, sem tocar no Repository (os módulos só falam com Services). */
@Service
public class UsuarioService {

	private final UsuarioRepository usuarios;

	public UsuarioService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	/** O fuso horário do usuário: define o "hoje" e o dia de criação e exclusão dos hábitos nos Insights. */
	@Transactional(readOnly = true)
	public ZoneId fusoDe(Long usuarioId) {
		return usuarios.findById(usuarioId)
				.map(usuario -> ZoneId.of(usuario.getFuso()))
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

}
