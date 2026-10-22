package com.ensora.auth;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarios;

	@InjectMocks
	private UsuarioService servico;

	private Usuario usuarioComFuso(String fuso) {
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setFuso(fuso);
		return usuario;
	}

	@Test
	void fusoDeDevolveOFusoGuardadoNaConta() {
		when(usuarios.findById(7L)).thenReturn(Optional.of(usuarioComFuso("America/Manaus")));

		assertThat(servico.fusoDe(7L)).isEqualTo(ZoneId.of("America/Manaus"));
	}

	@Test
	void usuarioNovoSemFusoInformadoFicaEmSaoPaulo() {
		assertThat(new Usuario().getFuso()).isEqualTo("America/Sao_Paulo");
	}

	@Test
	void fusoDeUsuarioInexistenteLancaNaoEncontrado() {
		when(usuarios.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico.fusoDe(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

}
