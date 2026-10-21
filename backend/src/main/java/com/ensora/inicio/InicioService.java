package com.ensora.inicio;

import com.ensora.entrega.EntregaService;
import com.ensora.evento.EventoService;
import com.ensora.habito.HabitoService;
import com.ensora.inicio.dto.InicioResponse;
import com.ensora.inicio.dto.InicioResponse.HabitoNaHome;
import com.ensora.inicio.dto.InicioResponse.Upcoming;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Leitura agregada da Home: só orquestra Services de outros módulos, nunca Repositories. */
@Service
public class InicioService {

	/** O Upcoming mostra de hoje até este número de dias à frente, inclusive (seção 8). */
	private static final long DIAS_DO_UPCOMING = 4;

	private final HabitoService habitoService;
	private final EntregaService entregaService;
	private final EventoService eventoService;

	public InicioService(HabitoService habitoService, EntregaService entregaService, EventoService eventoService) {
		this.habitoService = habitoService;
		this.entregaService = entregaService;
		this.eventoService = eventoService;
	}

	public InicioResponse carregar(Long usuarioId, LocalDate data) {
		// Os hábitos vêm primeiro porque é ele quem confere se a data está perto do relógio do servidor (400).
		var habitos = habitoService.listarDoDia(usuarioId, data).stream().map(HabitoNaHome::de).toList();

		LocalDate ate = data.plusDays(DIAS_DO_UPCOMING);
		// Entrega atrasada ou concluída fica na Home até o dia seguinte; por isso a entrega olha desde ontem.
		var entregas = entregaService.listarParaHome(usuarioId, data.minusDays(1), ate);
		var eventos = eventoService.listarNoIntervalo(usuarioId, data, ate);

		return new InicioResponse(data, habitos, new Upcoming(entregas, eventos));
	}

}
