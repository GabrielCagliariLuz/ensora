package com.ensora.conta;

import com.ensora.auth.LimitadorDeTentativasLogin;
import com.ensora.auth.UsuarioService;
import com.ensora.compartilhado.data.FusoHorario;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.conta.dto.ContaResponse;
import com.ensora.entrega.EntregaService;
import com.ensora.evento.EventoService;
import com.ensora.habito.HabitoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A conta de quem está logado: ver, trocar o fuso e a senha e excluir tudo. Módulo agregado, como {@code inicio} e
 * {@code insights}: orquestra Services de outros módulos, nunca Repositories.
 */
@Service
public class ContaService {

	private final UsuarioService usuarioService;
	private final HabitoService habitoService;
	private final EntregaService entregaService;
	private final EventoService eventoService;
	private final LimitadorDeTentativasLogin limitador;

	public ContaService(UsuarioService usuarioService, HabitoService habitoService, EntregaService entregaService,
			EventoService eventoService, LimitadorDeTentativasLogin limitador) {
		this.usuarioService = usuarioService;
		this.habitoService = habitoService;
		this.entregaService = entregaService;
		this.eventoService = eventoService;
		this.limitador = limitador;
	}

	@Transactional(readOnly = true)
	public ContaResponse buscar(Long usuarioId) {
		return ContaResponse.de(usuarioService.buscar(usuarioId));
	}

	@Transactional
	public ContaResponse alterarFuso(Long usuarioId, String fuso) {
		return ContaResponse.de(usuarioService.alterarFuso(usuarioId, FusoHorario.interpretar(fuso)));
	}

	/** Registra que a pessoa viu (ou pulou) o tutorial de primeiro acesso; o front não o mostra de novo. */
	@Transactional
	public ContaResponse concluirIntroducao(Long usuarioId) {
		return ContaResponse.de(usuarioService.concluirIntroducao(usuarioId));
	}

	/** Exige a senha atual e encerra todas as sessões; a pessoa entra de novo com a senha nova. */
	@Transactional
	public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha, String ip) {
		exigirSenha(usuarioId, senhaAtual, "senhaAtual", "senha atual incorreta", ip);
		if (usuarioService.senhaConfere(usuarioId, novaSenha)) {
			throw new ValidacaoException("novaSenha", "a nova senha deve ser diferente da atual");
		}
		usuarioService.alterarSenha(usuarioId, novaSenha);
	}

	/** Apaga tudo o que é do usuário e depois a conta, numa transação só: ou some tudo, ou nada. */
	@Transactional
	public void excluir(Long usuarioId, String senha, String ip) {
		exigirSenha(usuarioId, senha, "senha", "senha incorreta", ip);
		habitoService.apagarTodosDoUsuario(usuarioId);
		entregaService.apagarTodasDoUsuario(usuarioId);
		eventoService.apagarTodosDoUsuario(usuarioId);
		usuarioService.excluir(usuarioId);
	}

	/**
	 * Quem já tem o access token (por exemplo, roubado) poderia chutar a senha à vontade: errar a senha conta no mesmo
	 * limite por IP do login. A resposta é 400 no campo, e não 401, para o front não confundir com sessão perdida.
	 */
	private void exigirSenha(Long usuarioId, String senha, String campo, String mensagem, String ip) {
		limitador.verificar(ip);
		if (!usuarioService.senhaConfere(usuarioId, senha)) {
			limitador.registrarFalha(ip);
			throw new ValidacaoException(campo, mensagem);
		}
	}

}
