package com.ensora.auth;

import com.ensora.auth.dto.LoginRequest;
import com.ensora.auth.dto.RegistrarRequest;
import com.ensora.auth.dto.UsuarioResponse;
import com.ensora.compartilhado.erro.AcessoNegadoException;
import com.ensora.compartilhado.erro.ConflitoException;
import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.NaoAutenticadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String IP = "203.0.113.7";

	@Mock
	private UsuarioRepository usuarios;

	@Mock
	private TokenAcessoService tokenAcessoService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private final PasswordEncoder codificadorDeSenha = new BCryptPasswordEncoder();

	// O limitador é de verdade (não simulado) para os testes provarem o bloqueio de ponta a ponta.
	private final LimitadorDeTentativasLogin limitador =
			new LimitadorDeTentativasLogin(Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC), 5, 15);

	private AuthService servico(boolean registroHabilitado) {
		return new AuthService(usuarios, codificadorDeSenha, tokenAcessoService, refreshTokenService,
				limitador, registroHabilitado);
	}

	private Usuario usuarioComSenha(String senha) {
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setEmail("a@b.com");
		usuario.setSenhaHash(codificadorDeSenha.encode(senha));
		return usuario;
	}

	private void falharLogin(AuthService servico, int vezes) {
		for (int i = 0; i < vezes; i++) {
			assertThatThrownBy(() -> servico.login(new LoginRequest("a@b.com", "errada"), IP))
					.isInstanceOf(NaoAutenticadoException.class);
		}
	}

	@Test
	void registrarComCadastroDesabilitadoLancaAcessoNegado() {
		AuthService servico = servico(false);

		assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "1234")))
				.isInstanceOf(AcessoNegadoException.class)
				.hasMessage("Cadastro desabilitado");
		verifyNoInteractions(usuarios);
	}

	@Test
	void registrarSalvaEmailEmMinusculasESemEspacos() {
		when(usuarios.existsByEmail("gabriel@gmail.com")).thenReturn(false);
		when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> {
			Usuario salvo = invocacao.getArgument(0);
			salvo.setId(7L);
			return salvo;
		});

		UsuarioResponse resposta = servico(true).registrar(new RegistrarRequest("  Gabriel@Gmail.COM ", "1234"));

		assertThat(resposta.id()).isEqualTo(7L);
		assertThat(resposta.email()).isEqualTo("gabriel@gmail.com");
	}

	@Test
	void registrarComEmailJaCadastradoLancaConflito() {
		when(usuarios.existsByEmail("a@b.com")).thenReturn(true);

		assertThatThrownBy(() -> servico(true).registrar(new RegistrarRequest("A@B.com", "1234")))
				.isInstanceOf(ConflitoException.class)
				.hasMessage("E-mail já cadastrado");
		verify(usuarios, never()).save(any());
	}

	@Test
	void registrarGuardaHashBcryptENaoASenha() {
		when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico(true).registrar(new RegistrarRequest("a@b.com", "senha-secreta"));

		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarios).save(capturado.capture());
		String hash = capturado.getValue().getSenhaHash();
		assertThat(hash).isNotEqualTo("senha-secreta").startsWith("$2");
		assertThat(codificadorDeSenha.matches("senha-secreta", hash)).isTrue();
	}

	@Test
	void registrarTraduzViolacaoDeUnicidadeDoBancoEmConflito() {
		when(usuarios.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("uk_usuario_email"));

		assertThatThrownBy(() -> servico(true).registrar(new RegistrarRequest("a@b.com", "1234")))
				.isInstanceOf(ConflitoException.class);
	}

	@Test
	void loginComCredenciaisValidasDevolveAccessERefreshToken() {
		when(usuarios.findByEmail("a@b.com")).thenReturn(Optional.of(usuarioComSenha("1234")));
		when(tokenAcessoService.gerar(7L)).thenReturn(new TokenAcesso("jwt", 900));
		when(refreshTokenService.criar(7L)).thenReturn(new RefreshTokenEmitido("refresh", Duration.ofDays(30)));

		SessaoCriada sessao = servico(true).login(new LoginRequest("  A@B.com ", "1234"), IP);

		assertThat(sessao.tokenAcesso().valor()).isEqualTo("jwt");
		assertThat(sessao.refreshToken().valor()).isEqualTo("refresh");
	}

	@Test
	void loginComSenhaErradaDevolveErroGenerico() {
		when(usuarios.findByEmail("a@b.com")).thenReturn(Optional.of(usuarioComSenha("1234")));

		assertThatThrownBy(() -> servico(true).login(new LoginRequest("a@b.com", "errada"), IP))
				.isInstanceOf(NaoAutenticadoException.class)
				.hasMessage("E-mail ou senha inválidos");
		verifyNoInteractions(tokenAcessoService, refreshTokenService);
	}

	@Test
	void loginComEmailInexistenteDevolveOMesmoErroGenerico() {
		when(usuarios.findByEmail("fantasma@b.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico(true).login(new LoginRequest("fantasma@b.com", "1234"), IP))
				.isInstanceOf(NaoAutenticadoException.class)
				.hasMessage("E-mail ou senha inválidos");
		verifyNoInteractions(tokenAcessoService, refreshTokenService);
	}

	@Test
	void loginAposCincoFalhasDoMesmoIpEhBloqueado() {
		when(usuarios.findByEmail("a@b.com")).thenReturn(Optional.of(usuarioComSenha("1234")));
		AuthService servico = servico(true);
		falharLogin(servico, 5);

		// Mesmo com a senha certa: o IP está bloqueado.
		assertThatThrownBy(() -> servico.login(new LoginRequest("a@b.com", "1234"), IP))
				.isInstanceOf(LimiteExcedidoException.class);
	}

	@Test
	void falhasDeUmIpNaoBloqueiamOutroIp() {
		when(usuarios.findByEmail("a@b.com")).thenReturn(Optional.of(usuarioComSenha("1234")));
		when(tokenAcessoService.gerar(7L)).thenReturn(new TokenAcesso("jwt", 900));
		when(refreshTokenService.criar(7L)).thenReturn(new RefreshTokenEmitido("refresh", Duration.ofDays(30)));
		AuthService servico = servico(true);
		falharLogin(servico, 5);

		SessaoCriada sessao = servico.login(new LoginRequest("a@b.com", "1234"), "198.51.100.9");

		assertThat(sessao.tokenAcesso().valor()).isEqualTo("jwt");
	}

	@Test
	void loginComSucessoZeraOContadorDeFalhasDoIp() {
		when(usuarios.findByEmail("a@b.com")).thenReturn(Optional.of(usuarioComSenha("1234")));
		when(tokenAcessoService.gerar(7L)).thenReturn(new TokenAcesso("jwt", 900));
		when(refreshTokenService.criar(7L)).thenReturn(new RefreshTokenEmitido("refresh", Duration.ofDays(30)));
		AuthService servico = servico(true);

		falharLogin(servico, 4);
		servico.login(new LoginRequest("a@b.com", "1234"), IP);
		// Se o contador não tivesse zerado, a 5ª falha daqui para frente já bloquearia.
		falharLogin(servico, 4);

		SessaoCriada sessao = servico.login(new LoginRequest("a@b.com", "1234"), IP);
		assertThat(sessao.tokenAcesso().valor()).isEqualTo("jwt");
	}

}
