package com.ensora.evento;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.evento.dto.EventoRequest;
import com.ensora.evento.dto.EventoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventoService {

	private final EventoRepository eventos;

	public EventoService(EventoRepository eventos) {
		this.eventos = eventos;
	}

	@Transactional
	public EventoResponse criar(Long usuarioId, EventoRequest requisicao) {
		validarTermino(requisicao);
		Evento evento = new Evento();
		evento.setUsuarioId(usuarioId);
		preencher(evento, requisicao);
		return EventoResponse.de(eventos.save(evento));
	}

	/** Substitui o evento inteiro: termino, local e categoria ausentes na requisição são limpos. */
	@Transactional
	public EventoResponse atualizar(Long usuarioId, Long id, EventoRequest requisicao) {
		Evento evento = buscarDoUsuario(usuarioId, id);
		validarTermino(requisicao);
		// O evento foi carregado nesta transação: o JPA grava a mudança sozinho, ao final.
		preencher(evento, requisicao);
		return EventoResponse.de(evento);
	}

	/** Eventos do usuário de {@code de} a {@code ate}, inclusive nas duas pontas. Serve à Home e ao Calendar. */
	@Transactional(readOnly = true)
	public List<EventoResponse> listarNoIntervalo(Long usuarioId, LocalDate de, LocalDate ate) {
		return eventos.findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(usuarioId, de, ate).stream()
				.map(EventoResponse::de)
				.toList();
	}

	/** Exclusão física: evento não tem histórico a preservar (seção 9). */
	@Transactional
	public void excluir(Long usuarioId, Long id) {
		eventos.delete(buscarDoUsuario(usuarioId, id));
	}

	// Evento de outro usuário ou inexistente dão o mesmo 404: não revela se o id existe para outra pessoa.
	private Evento buscarDoUsuario(Long usuarioId, Long id) {
		return eventos.findByIdAndUsuarioId(id, usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado"));
	}

	// O CHECK do banco também confere, mas barrar aqui devolve 400 em vez de um erro de integridade (500).
	// Início e término são TIME do mesmo dia, então termino > inicio basta para o evento não atravessar a meia-noite.
	private void validarTermino(EventoRequest requisicao) {
		if (requisicao.termino() != null && !requisicao.termino().isAfter(requisicao.inicio())) {
			throw new ValidacaoException("termino", "deve ser depois do início");
		}
	}

	private void preencher(Evento evento, EventoRequest requisicao) {
		evento.setTitulo(requisicao.titulo().trim());
		evento.setData(requisicao.data());
		evento.setInicio(requisicao.inicio());
		evento.setTermino(requisicao.termino());
		// Local só com espaços vale como ausente.
		String local = requisicao.local() == null ? null : requisicao.local().trim();
		evento.setLocal(local == null || local.isEmpty() ? null : local);
		evento.setCategoria(requisicao.categoria());
	}

	/** Apaga todos os eventos do usuário. Só a exclusão da conta usa isto. */
	@Transactional
	public void apagarTodosDoUsuario(Long usuarioId) {
		eventos.apagarDoUsuario(usuarioId);
	}

}
