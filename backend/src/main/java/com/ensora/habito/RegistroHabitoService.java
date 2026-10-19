package com.ensora.habito;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class RegistroHabitoService {

	/**
	 * A data de "hoje" vem do front (a data do registro também). O servidor só recusa uma data futura
	 * se ela passar de 2 dias do relógio dele, o que cobre qualquer fuso. Datas passadas não têm limite.
	 */
	private static final long TOLERANCIA_DE_DATA_FUTURA_EM_DIAS = 2;

	private final HabitoRepository habitos;
	private final RegistroHabitoRepository registros;
	private final Clock relogio;

	public RegistroHabitoService(HabitoRepository habitos, RegistroHabitoRepository registros, Clock relogio) {
		this.habitos = habitos;
		this.registros = registros;
		this.relogio = relogio;
	}

	/** Idempotente: marcar o que já está marcado não gera erro nem duplicata. */
	@Transactional
	public void marcar(Long usuarioId, Long habitoId, LocalDate data) {
		validarQueNaoEhFutura(data);
		Habito habito = buscarDoUsuario(usuarioId, habitoId);
		registros.marcar(habito.getId(), data);
	}

	/** Idempotente: desmarcar o que não está marcado não gera erro. */
	@Transactional
	public void desmarcar(Long usuarioId, Long habitoId, LocalDate data) {
		Habito habito = buscarDoUsuario(usuarioId, habitoId);
		registros.desmarcar(habito.getId(), data);
	}

	// Hábito de outro usuário, inexistente ou excluído dão o mesmo 404.
	private Habito buscarDoUsuario(Long usuarioId, Long habitoId) {
		return habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(habitoId, usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Hábito não encontrado"));
	}

	private void validarQueNaoEhFutura(LocalDate data) {
		long diasNoFuturo = ChronoUnit.DAYS.between(LocalDate.now(relogio), data);
		if (diasNoFuturo > TOLERANCIA_DE_DATA_FUTURA_EM_DIAS) {
			throw new ValidacaoException("data", "não pode ser uma data futura");
		}
	}

}
