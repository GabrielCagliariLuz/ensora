package com.ensora.calendario;

import com.ensora.calendario.dto.CalendarioResponse;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.entrega.EntregaService;
import com.ensora.evento.EventoService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Leitura agregada do Calendar: só orquestra Services de outros módulos, nunca Repositories. */
@Service
public class CalendarioService {

	/**
	 * Tamanho máximo do intervalo, em dias entre {@code de} e {@code ate}. A grade mensal tem no máximo 42 dias;
	 * o limite evita pedir anos de dados numa chamada só.
	 */
	private static final long INTERVALO_MAXIMO_EM_DIAS = 62;

	private final EntregaService entregaService;
	private final EventoService eventoService;

	public CalendarioService(EntregaService entregaService, EventoService eventoService) {
		this.entregaService = entregaService;
		this.eventoService = eventoService;
	}

	public CalendarioResponse carregar(Long usuarioId, LocalDate de, LocalDate ate) {
		validarIntervalo(de, ate);
		return new CalendarioResponse(de, ate,
				entregaService.listarNoIntervalo(usuarioId, de, ate),
				eventoService.listarNoIntervalo(usuarioId, de, ate));
	}

	// O intervalo é de navegação (passado ou futuro): não há a regra de "perto de hoje" dos outros endpoints.
	private void validarIntervalo(LocalDate de, LocalDate ate) {
		if (ate.isBefore(de)) {
			throw new ValidacaoException("ate", "não pode ser antes de 'de'");
		}
		if (ChronoUnit.DAYS.between(de, ate) > INTERVALO_MAXIMO_EM_DIAS) {
			throw new ValidacaoException("ate", "intervalo maior que " + INTERVALO_MAXIMO_EM_DIAS + " dias");
		}
	}

}
