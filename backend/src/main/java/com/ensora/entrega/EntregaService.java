package com.ensora.entrega;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.entrega.dto.EntregaRequest;
import com.ensora.entrega.dto.EntregaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class EntregaService {

	private final EntregaRepository entregas;
	private final Clock relogio;

	public EntregaService(EntregaRepository entregas, Clock relogio) {
		this.entregas = entregas;
		this.relogio = relogio;
	}

	@Transactional
	public EntregaResponse criar(Long usuarioId, EntregaRequest requisicao) {
		Entrega entrega = new Entrega();
		entrega.setUsuarioId(usuarioId);
		preencher(entrega, requisicao);
		return EntregaResponse.de(entregas.save(entrega));
	}

	/** Troca título, prazo e nota. A conclusão não muda aqui: só o {@link #alterarConclusao}. */
	@Transactional
	public EntregaResponse atualizar(Long usuarioId, Long id, EntregaRequest requisicao) {
		Entrega entrega = buscarDoUsuario(usuarioId, id);
		// A entrega foi carregada nesta transação: o JPA grava a mudança sozinho, ao final.
		preencher(entrega, requisicao);
		return EntregaResponse.de(entrega);
	}

	/**
	 * Idempotente: concluir o que já está concluído mantém a hora original; desfazer o que não está concluído não
	 * faz nada. {@code concluida_em} vem do relógio do servidor e sempre acompanha {@code concluida} (o banco confere).
	 */
	@Transactional
	public EntregaResponse alterarConclusao(Long usuarioId, Long id, boolean concluida) {
		Entrega entrega = buscarDoUsuario(usuarioId, id);
		if (concluida && !entrega.isConcluida()) {
			entrega.setConcluida(true);
			entrega.setConcluidaEm(relogio.instant());
		} else if (!concluida && entrega.isConcluida()) {
			entrega.setConcluida(false);
			entrega.setConcluidaEm(null);
		}
		return EntregaResponse.de(entrega);
	}

	/** Exclusão física: entrega não tem histórico a preservar (decisão 49). */
	@Transactional
	public void excluir(Long usuarioId, Long id) {
		entregas.delete(buscarDoUsuario(usuarioId, id));
	}

	// Entrega de outro usuário ou inexistente dão o mesmo 404: não revela se o id existe para outra pessoa.
	private Entrega buscarDoUsuario(Long usuarioId, Long id) {
		return entregas.findByIdAndUsuarioId(id, usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada"));
	}

	private void preencher(Entrega entrega, EntregaRequest requisicao) {
		entrega.setTitulo(requisicao.titulo().trim());
		entrega.setPrazoData(requisicao.prazoData());
		entrega.setPrazoHorario(requisicao.prazoHorario());
		// Nota só com espaços vale como ausente.
		String nota = requisicao.nota() == null ? null : requisicao.nota().trim();
		entrega.setNota(nota == null || nota.isEmpty() ? null : nota);
	}

}
