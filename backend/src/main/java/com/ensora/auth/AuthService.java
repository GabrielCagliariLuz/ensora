package com.ensora.auth;

import com.ensora.auth.dto.ConfiguracaoCadastroResponse;
import com.ensora.auth.dto.LoginRequest;
import com.ensora.auth.dto.RegistrarRequest;
import com.ensora.auth.dto.UsuarioResponse;
import com.ensora.compartilhado.erro.AcessoNegadoException;
import com.ensora.compartilhado.erro.ConflitoException;
import com.ensora.compartilhado.erro.NaoAutenticadoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class AuthService {

	private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha inválidos";
	private static final String MENSAGEM_SESSAO_INVALIDA = "Sessão inválida";

	private final UsuarioRepository usuarios;
	private final PasswordEncoder codificadorDeSenha;
	private final TokenAcessoService tokenAcessoService;
	private final RefreshTokenService refreshTokenService;
	private final LimitadorDeTentativasLogin limitador;
	private final LimitadorDeCadastros limitadorDeCadastros;
	private final boolean registroHabilitado;
	// Vazio: cadastro sem convite. Preenchido: só entra quem informar este código.
	private final String conviteCodigo;
	// Hash de uma senha que ninguém tem: serve para gastar o mesmo tempo quando o e-mail não existe.
	private final String hashFicticio;

	public AuthService(UsuarioRepository usuarios, PasswordEncoder codificadorDeSenha,
			TokenAcessoService tokenAcessoService, RefreshTokenService refreshTokenService,
			LimitadorDeTentativasLogin limitador, LimitadorDeCadastros limitadorDeCadastros,
			@Value("${ensora.auth.registro-habilitado}") boolean registroHabilitado,
			@Value("${ensora.auth.convite-codigo}") String conviteCodigo) {
		this.usuarios = usuarios;
		this.codificadorDeSenha = codificadorDeSenha;
		this.tokenAcessoService = tokenAcessoService;
		this.refreshTokenService = refreshTokenService;
		this.limitador = limitador;
		this.limitadorDeCadastros = limitadorDeCadastros;
		this.registroHabilitado = registroHabilitado;
		this.conviteCodigo = conviteCodigo == null ? "" : conviteCodigo.trim();
		this.hashFicticio = codificadorDeSenha.encode("senha-que-ninguem-tem");
	}

	/** O cadastro aberto ao público depende desta flag; a tela consulta {@link #configuracaoDeCadastro()}. */
	public ConfiguracaoCadastroResponse configuracaoDeCadastro() {
		return new ConfiguracaoCadastroResponse(registroHabilitado, !conviteCodigo.isEmpty());
	}

	public UsuarioResponse registrar(RegistrarRequest requisicao, String ip) {
		if (!registroHabilitado) {
			throw new AcessoNegadoException("Cadastro desabilitado");
		}
		limitadorDeCadastros.verificar(ip);
		// Toda tentativa conta, certa ou errada: limita a criação de contas e o palpite do código de convite.
		limitadorDeCadastros.registrarTentativa(ip);
		exigirConvite(requisicao.codigoConvite());
		String email = normalizar(requisicao.email());
		if (usuarios.existsByEmail(email)) {
			throw new ConflitoException("E-mail já cadastrado");
		}
		Usuario usuario = new Usuario();
		usuario.setEmail(email);
		usuario.setSenhaHash(codificadorDeSenha.encode(requisicao.senha()));
		try {
			usuario = usuarios.save(usuario);
		} catch (DataIntegrityViolationException e) {
			// Dois pedidos simultâneos: o existsByEmail passou nos dois, mas o UNIQUE do banco barrou o segundo.
			throw new ConflitoException("E-mail já cadastrado");
		}
		return UsuarioResponse.de(usuario);
	}

	public SessaoCriada login(LoginRequest requisicao, String ip) {
		limitador.verificar(ip);

		Usuario usuario = usuarios.findByEmail(normalizar(requisicao.email())).orElse(null);
		// Compara sempre com um hash, exista o e-mail ou não, para o tempo de resposta não denunciar quais e-mails existem.
		String hashParaComparar = usuario != null ? usuario.getSenhaHash() : hashFicticio;
		boolean senhaConfere = codificadorDeSenha.matches(requisicao.senha(), hashParaComparar);

		if (usuario == null || !senhaConfere) {
			limitador.registrarFalha(ip);
			throw new NaoAutenticadoException(MENSAGEM_LOGIN_INVALIDO);
		}

		limitador.limpar(ip);
		TokenAcesso tokenAcesso = tokenAcessoService.gerar(usuario.getId());
		RefreshTokenEmitido refreshToken = refreshTokenService.criar(usuario.getId());
		return new SessaoCriada(tokenAcesso, refreshToken);
	}

	public SessaoCriada refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new NaoAutenticadoException(MENSAGEM_SESSAO_INVALIDA);
		}
		RotacaoRefresh rotacao = refreshTokenService.rotacionar(refreshToken);
		TokenAcesso tokenAcesso = tokenAcessoService.gerar(rotacao.usuarioId());
		return new SessaoCriada(tokenAcesso, rotacao.novoRefreshToken());
	}

	public void logout(String refreshToken) {
		// Sem cookie não há o que revogar: o logout continua sendo bem-sucedido.
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		refreshTokenService.revogar(refreshToken);
	}

	// Antes de olhar o e-mail, para quem não tem o código não descobrir se um e-mail já existe.
	private void exigirConvite(String informado) {
		if (conviteCodigo.isEmpty()) {
			return;
		}
		byte[] esperado = conviteCodigo.getBytes(StandardCharsets.UTF_8);
		byte[] recebido = (informado == null ? "" : informado.trim()).getBytes(StandardCharsets.UTF_8);
		// Comparação em tempo constante, para o tempo de resposta não denunciar quantos caracteres batem.
		if (!MessageDigest.isEqual(esperado, recebido)) {
			throw new ValidacaoException("codigoConvite", "Código de convite inválido");
		}
	}

	private String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
