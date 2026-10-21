package com.ensora.entrega;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.entrega.dto.EntregaRequest;
import com.ensora.entrega.dto.EntregaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EntregaService {

	private static final long TOLERANCIA_DA_DATA_EM_DIAS = 2;

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
	 * Idempotente: concluir o que já está concluído mantém a hora e a data originais; desfazer o que não está
	 * concluído não faz nada. {@code concluida_em} vem do relógio do servidor; {@code concluida_data} é o "hoje" que o
	 * front enviou (obrigatório ao concluir, ignorado ao desfazer). Os três campos sempre andam juntos (o banco confere).
	 */
	@Transactional
	public EntregaResponse alterarConclusao(Long usuarioId, Long id, boolean concluida, LocalDate data) {
		if (concluida) {
			validarData(data);
		}
		Entrega entrega = buscarDoUsuario(usuarioId, id);
		if (concluida && !entrega.isConcluida()) {
			entrega.setConcluida(true);
			entrega.setConcluidaEm(relogio.instant());
			entrega.setConcluidaData(data);
		} else if (!concluida && entrega.isConcluida()) {
			entrega.setConcluida(false);
			entrega.setConcluidaEm(null);
			entrega.setConcluidaData(null);
		}
		return EntregaResponse.de(entrega);
	}

	/**
	 * Entregas da Home. {@code desde} é ontem: uma entrega atrasada fica na Home só até o dia seguinte ao prazo, e uma
	 * concluída só até o dia seguinte à conclusão; depois disso só o Calendar a mostra (seção 8).
	 */
	@Transactional(readOnly = true)
	public List<EntregaResponse> listarParaHome(Long usuarioId, LocalDate desde, LocalDate ate) {
		return entregas.findParaHome(usuarioId, desde, ate).stream().map(EntregaResponse::de).toList();
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

	// O "hoje" vem do front; o servidor só recusa se estiver longe do relógio dele (2 dias cobrem qualquer fuso).
	private void validarData(LocalDate data) {
		if (data == null) {
			throw new ValidacaoException("data", "obrigatória ao concluir");
		}
		long diferenca = Math.abs(ChronoUnit.DAYS.between(LocalDate.now(relogio), data));
		if (diferenca > TOLERANCIA_DA_DATA_EM_DIAS) {
			throw new ValidacaoException("data", "muito distante da data de hoje");
		}
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
