package com.ensora.auth;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Dados da conta que outros módulos precisam, sem tocar no Repository (os módulos só falam com Services). */
@Service
public class UsuarioService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder codificadorDeSenha;
	private final RefreshTokenService refreshTokenService;
	private final Clock relogio;

	public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificadorDeSenha,
			RefreshTokenService refreshTokenService, Clock relogio) {
		this.usuarios = usuarios;
		this.codificadorDeSenha = codificadorDeSenha;
		this.refreshTokenService = refreshTokenService;
		this.relogio = relogio;
	}

	@Transactional(readOnly = true)
	public Usuario buscar(Long usuarioId) {
		return usuarios.findById(usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	/** O fuso horário do usuário: define o "hoje" e o dia de criação e exclusão dos hábitos nos Insights. */
	@Transactional(readOnly = true)
	public ZoneId fusoDe(Long usuarioId) {
		return ZoneId.of(buscar(usuarioId).getFuso());
	}

	@Transactional
	public Usuario alterarFuso(Long usuarioId, ZoneId fuso) {
		Usuario usuario = buscar(usuarioId);
		// O usuário foi carregado nesta transação: o JPA grava a mudança sozinho, ao final.
		usuario.setFuso(fuso.getId());
		return usuario;
	}

	/** Marca o tutorial de primeiro acesso como visto. Idempotente: repetir não muda a data da primeira vez. */
	@Transactional
	public Usuario concluirIntroducao(Long usuarioId) {
		Usuario usuario = buscar(usuarioId);
		if (usuario.getIntroducaoConcluidaEm() == null) {
			usuario.setIntroducaoConcluidaEm(Instant.now(relogio));
		}
		return usuario;
	}

	@Transactional(readOnly = true)
	public boolean senhaConfere(Long usuarioId, String senha) {
		return codificadorDeSenha.matches(senha, buscar(usuarioId).getSenhaHash());
	}

	/**
	 * Troca a senha e encerra todas as sessões. O refresh token vai num cookie de {@code Path=/auth}, que esta rota não
	 * recebe, então não dá para poupar a sessão atual: o front leva a pessoa de volta ao login. (O access token já
	 * emitido segue valendo até expirar, em até 15 minutos.)
	 */
	@Transactional
	public void alterarSenha(Long usuarioId, String novaSenha) {
		Usuario usuario = buscar(usuarioId);
		usuario.setSenhaHash(codificadorDeSenha.encode(novaSenha));
		refreshTokenService.revogarTodas(usuarioId);
	}

	/** Apaga as sessões e a conta. Os dados do usuário em outros módulos precisam ter sido apagados antes. */
	@Transactional
	public void excluir(Long usuarioId) {
		Usuario usuario = buscar(usuarioId);
		refreshTokenService.apagarTodas(usuarioId);
		usuarios.delete(usuario);
	}

}
