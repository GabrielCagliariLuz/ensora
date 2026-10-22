package com.ensora.auth;

import com.ensora.auth.dto.LoginRequest;
import com.ensora.auth.dto.RegistrarRequest;
import com.ensora.auth.dto.UsuarioResponse;
import com.ensora.compartilhado.erro.AcessoNegadoException;
import com.ensora.compartilhado.erro.ConflitoException;
import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.NaoAutenticadoException;
import com.ensora.compartilhado.erro.ValidacaoException;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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
	private final Clock relogio = Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC);

	private final LimitadorDeTentativasLogin limitador = new LimitadorDeTentativasLogin(relogio, 5, 15);

	// Três tentativas por janela: pequeno o bastante para os testes provarem o bloqueio.
	private final LimitadorDeCadastros limitadorDeCadastros = new LimitadorDeCadastros(relogio, 3, 60);

	private static final String CONVITE = "convite-da-familia";

	private AuthService servico(boolean registroHabilitado) {
		return servico(registroHabilitado, "");
	}

	private AuthService servico(boolean registroHabilitado, String conviteCodigo) {
		return new AuthService(usuarios, codificadorDeSenha, tokenAcessoService, refreshTokenService,
				limitador, limitadorDeCadastros, registroHabilitado, conviteCodigo, "America/Sao_Paulo");
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

		assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "1234", null, null), IP))
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

		UsuarioResponse resposta = servico(true).registrar(new RegistrarRequest("  Gabriel@Gmail.COM ", "1234", null, null), IP);

		assertThat(resposta.id()).isEqualTo(7L);
		assertThat(resposta.email()).isEqualTo("gabriel@gmail.com");
	}

	@Test
	void registrarComEmailJaCadastradoLancaConflito() {
		when(usuarios.existsByEmail("a@b.com")).thenReturn(true);

		assertThatThrownBy(() -> servico(true).registrar(new RegistrarRequest("A@B.com", "1234", null, null), IP))
				.isInstanceOf(ConflitoException.class)
				.hasMessage("E-mail já cadastrado");
		verify(usuarios, never()).save(any());
	}

	@Test
	void registrarGuardaHashBcryptENaoASenha() {
		when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico(true).registrar(new RegistrarRequest("a@b.com", "senha-secreta", null, null), IP);

		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarios).save(capturado.capture());
		String hash = capturado.getValue().getSenhaHash();
		assertThat(hash).isNotEqualTo("senha-secreta").startsWith("$2");
		assertThat(codificadorDeSenha.matches("senha-secreta", hash)).isTrue();
	}

	@Test
	void registrarTraduzViolacaoDeUnicidadeDoBancoEmConflito() {
		when(usuarios.save(any(Usuario.class))).thenThrow(new DataIntegrityViolationException("uk_usuario_email"));

		assertThatThrownBy(() -> servico(true).registrar(new RegistrarRequest("a@b.com", "1234", null, null), IP))
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

	@Test
	void refreshDevolveNovoAccessTokenENovoRefreshToken() {
		RefreshTokenEmitido novo = new RefreshTokenEmitido("refresh-novo", Duration.ofDays(30));
		when(refreshTokenService.rotacionar("refresh-antigo")).thenReturn(new RotacaoRefresh(7L, novo));
		when(tokenAcessoService.gerar(7L)).thenReturn(new TokenAcesso("jwt-novo", 900));

		SessaoCriada sessao = servico(true).refresh("refresh-antigo");

		assertThat(sessao.tokenAcesso().valor()).isEqualTo("jwt-novo");
		assertThat(sessao.refreshToken().valor()).isEqualTo("refresh-novo");
	}

	@Test
	void refreshSemTokenLancaNaoAutenticadoSemConsultarOBanco() {
		assertThatThrownBy(() -> servico(true).refresh(null))
				.isInstanceOf(NaoAutenticadoException.class)
				.hasMessage("Sessão inválida");
		assertThatThrownBy(() -> servico(true).refresh("  "))
				.isInstanceOf(NaoAutenticadoException.class);
		verifyNoInteractions(refreshTokenService, tokenAcessoService);
	}

	@Test
	void logoutRevogaOTokenRecebido() {
		servico(true).logout("refresh-atual");

		verify(refreshTokenService).revogar("refresh-atual");
	}

	@Test
	void logoutSemTokenNaoFazNadaENaoFalha() {
		servico(true).logout(null);

		verifyNoInteractions(refreshTokenService);
	}

	// ---- cadastro: limite por IP e código de convite ----

	private void stubDeCadastroNovo() {
		when(usuarios.existsByEmail(any())).thenReturn(false);
		when(usuarios.save(any(Usuario.class))).thenAnswer(invocacao -> {
			Usuario salvo = invocacao.getArgument(0);
			salvo.setId(7L);
			return salvo;
		});
	}

	@Test
	void registrarBloqueiaDepoisDeTresTentativasDoMesmoIp() {
		when(usuarios.existsByEmail(any())).thenReturn(true);
		AuthService servico = servico(true);

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), IP))
					.isInstanceOf(ConflitoException.class);
		}

		assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), IP))
				.isInstanceOf(LimiteExcedidoException.class)
				.hasMessage("Muitas tentativas de cadastro. Tente novamente mais tarde.");
	}

	@Test
	void registrarNaoMisturaOContadorDeIpsDiferentes() {
		when(usuarios.existsByEmail(any())).thenReturn(true);
		AuthService servico = servico(true);

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), IP))
					.isInstanceOf(ConflitoException.class);
		}

		assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), "198.51.100.9"))
				.isInstanceOf(ConflitoException.class);
	}

	@Test
	void registrarComCadastroDesabilitadoNaoContaTentativa() {
		AuthService desabilitado = servico(false);
		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> desabilitado.registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), IP))
					.isInstanceOf(AcessoNegadoException.class);
		}

		// Nenhuma tentativa foi contada: o mesmo IP ainda não está bloqueado.
		assertThatCode(() -> limitadorDeCadastros.verificar(IP)).doesNotThrowAnyException();
	}

	@Test
	void registrarSemOCodigoQuandoOServidorExigeConviteLancaValidacao() {
		assertThatThrownBy(() -> servico(true, CONVITE).registrar(new RegistrarRequest("a@b.com", "senha-segura", null, null), IP))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> {
					assertThat(erro.getCampo()).isEqualTo("codigoConvite");
					assertThat(erro.getMessage()).isEqualTo("Código de convite inválido");
				});
		verifyNoInteractions(usuarios);
	}

	@Test
	void registrarComCodigoErradoLancaValidacaoENaoConsultaOBanco() {
		assertThatThrownBy(() -> servico(true, CONVITE).registrar(new RegistrarRequest("a@b.com", "senha-segura", "errado", null), IP))
				.isInstanceOf(ValidacaoException.class);
		verifyNoInteractions(usuarios);
	}

	@Test
	void registrarContaAsTentativasComCodigoErradoEBloqueiaOPalpite() {
		AuthService servico = servico(true, CONVITE);

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", "palpite", null), IP))
					.isInstanceOf(ValidacaoException.class);
		}

		// Mesmo com o código certo, o IP que ficou chutando já está bloqueado.
		assertThatThrownBy(() -> servico.registrar(new RegistrarRequest("a@b.com", "senha-segura", CONVITE, null), IP))
				.isInstanceOf(LimiteExcedidoException.class);
	}

	@Test
	void registrarComCodigoCertoCriaAConta() {
		stubDeCadastroNovo();

		UsuarioResponse resposta = servico(true, CONVITE)
				.registrar(new RegistrarRequest("a@b.com", "senha-segura", "  " + CONVITE + " ", null), IP);

		assertThat(resposta.id()).isEqualTo(7L);
	}

	@Test
	void registrarIgnoraOCodigoQuandoNaoHaConviteConfigurado() {
		stubDeCadastroNovo();

		UsuarioResponse resposta = servico(true)
				.registrar(new RegistrarRequest("a@b.com", "senha-segura", "qualquer-coisa", null), IP);

		assertThat(resposta.id()).isEqualTo(7L);
	}

	@Test
	void configuracaoDeCadastroInformaSeEstaAbertoESePedeConvite() {
		assertThat(servico(true).configuracaoDeCadastro().habilitado()).isTrue();
		assertThat(servico(true).configuracaoDeCadastro().exigeConvite()).isFalse();
		assertThat(servico(true, CONVITE).configuracaoDeCadastro().exigeConvite()).isTrue();
		assertThat(servico(false).configuracaoDeCadastro().habilitado()).isFalse();
		assertThat(servico(false).configuracaoDeCadastro().exigeConvite()).isFalse();
	}

	// ---- cadastro: fuso do usuário ----

	private Usuario cadastrarComFuso(String fusoInformado) {
		stubDeCadastroNovo();

		servico(true).registrar(new RegistrarRequest("a@b.com", "senha-segura", null, fusoInformado), IP);

		ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarios).save(capturado.capture());
		return capturado.getValue();
	}

	@Test
	void registrarGuardaOFusoInformadoPelaPessoa() {
		assertThat(cadastrarComFuso("America/Manaus").getFuso()).isEqualTo("America/Manaus");
	}

	@Test
	void registrarSemFusoUsaOPadraoDoServidor() {
		assertThat(cadastrarComFuso(null).getFuso()).isEqualTo("America/Sao_Paulo");
	}

	@Test
	void registrarComFusoEmBrancoUsaOPadraoDoServidor() {
		assertThat(cadastrarComFuso("  ").getFuso()).isEqualTo("America/Sao_Paulo");
	}

	@Test
	void registrarComFusoInvalidoLancaValidacaoNoCampoFusoENaoGravaNada() {
		assertThatThrownBy(() -> servico(true).registrar(new RegistrarRequest("a@b.com", "senha-segura", null, "Marte/Olimpo"), IP))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> assertThat(erro.getCampo()).isEqualTo("fuso"));
		verifyNoInteractions(usuarios);
	}

}
