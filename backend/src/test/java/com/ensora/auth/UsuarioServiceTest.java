package com.ensora.auth;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	private UsuarioRepository usuarios;

	@Mock
	private RefreshTokenService refreshTokenService;

	private final PasswordEncoder codificadorDeSenha = new BCryptPasswordEncoder();

	private final Instant agora = Instant.parse("2026-10-24T12:00:00Z");

	private UsuarioService servico() {
		return servico(Clock.fixed(agora, ZoneOffset.UTC));
	}

	private UsuarioService servico(Clock relogio) {
		return new UsuarioService(usuarios, codificadorDeSenha, refreshTokenService, relogio);
	}

	private Usuario usuario(String fuso, String senha) {
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setEmail("a@b.com");
		usuario.setFuso(fuso);
		usuario.setSenhaHash(codificadorDeSenha.encode(senha));
		return usuario;
	}

	@Test
	void fusoDeDevolveOFusoGuardadoNaConta() {
		when(usuarios.findById(7L)).thenReturn(Optional.of(usuario("America/Manaus", "senha-segura")));

		assertThat(servico().fusoDe(7L)).isEqualTo(ZoneId.of("America/Manaus"));
	}

	@Test
	void usuarioNovoSemFusoInformadoFicaEmSaoPaulo() {
		assertThat(new Usuario().getFuso()).isEqualTo("America/Sao_Paulo");
	}

	@Test
	void buscarDeUsuarioInexistenteLancaNaoEncontrado() {
		when(usuarios.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().buscar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
		assertThatThrownBy(() -> servico().fusoDe(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void alterarFusoGravaONomeDaRegiao() {
		Usuario atual = usuario("America/Sao_Paulo", "senha-segura");
		when(usuarios.findById(7L)).thenReturn(Optional.of(atual));

		Usuario alterado = servico().alterarFuso(7L, ZoneId.of("Asia/Tokyo"));

		assertThat(alterado.getFuso()).isEqualTo("Asia/Tokyo");
	}

	@Test
	void usuarioNovoAindaNaoConcluiuAIntroducao() {
		assertThat(new Usuario().getIntroducaoConcluidaEm()).isNull();
	}

	@Test
	void concluirIntroducaoGravaOMomentoDaConclusao() {
		Usuario atual = usuario("America/Sao_Paulo", "senha-segura");
		when(usuarios.findById(7L)).thenReturn(Optional.of(atual));

		Usuario concluido = servico().concluirIntroducao(7L);

		assertThat(concluido.getIntroducaoConcluidaEm()).isEqualTo(agora);
	}

	@Test
	void concluirIntroducaoDeNovoNaoMudaADataDaPrimeiraVez() {
		Usuario atual = usuario("America/Sao_Paulo", "senha-segura");
		when(usuarios.findById(7L)).thenReturn(Optional.of(atual));

		servico().concluirIntroducao(7L);
		Usuario repetido = servico(Clock.fixed(agora.plusSeconds(3600), ZoneOffset.UTC)).concluirIntroducao(7L);

		assertThat(repetido.getIntroducaoConcluidaEm()).isEqualTo(agora);
	}

	@Test
	void concluirIntroducaoDeUsuarioInexistenteLancaNaoEncontrado() {
		when(usuarios.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().concluirIntroducao(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
	}

	@Test
	void senhaConfereComparaComOHashBcrypt() {
		when(usuarios.findById(7L)).thenReturn(Optional.of(usuario("America/Sao_Paulo", "senha-segura")));

		assertThat(servico().senhaConfere(7L, "senha-segura")).isTrue();
		assertThat(servico().senhaConfere(7L, "outra-senha")).isFalse();
	}

	@Test
	void alterarSenhaGuardaOHashDaNovaSenhaERevogaTodasAsSessoes() {
		Usuario atual = usuario("America/Sao_Paulo", "senha-antiga");
		when(usuarios.findById(7L)).thenReturn(Optional.of(atual));

		servico().alterarSenha(7L, "senha-nova-123");

		assertThat(atual.getSenhaHash()).startsWith("$2");
		assertThat(codificadorDeSenha.matches("senha-nova-123", atual.getSenhaHash())).isTrue();
		assertThat(codificadorDeSenha.matches("senha-antiga", atual.getSenhaHash())).isFalse();
		verify(refreshTokenService).revogarTodas(7L);
	}

	@Test
	void excluirApagaAsSessoesAntesDaConta() {
		Usuario atual = usuario("America/Sao_Paulo", "senha-segura");
		when(usuarios.findById(7L)).thenReturn(Optional.of(atual));

		servico().excluir(7L);

		InOrder ordem = inOrder(refreshTokenService, usuarios);
		ordem.verify(refreshTokenService).apagarTodas(7L);
		ordem.verify(usuarios).delete(atual);
	}

	@Test
	void excluirUsuarioInexistenteNaoApagaNada() {
		when(usuarios.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().excluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
		verify(refreshTokenService, never()).apagarTodas(99L);
	}

}
