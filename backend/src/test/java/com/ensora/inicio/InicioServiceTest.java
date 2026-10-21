package com.ensora.inicio;

import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.entrega.EntregaService;
import com.ensora.entrega.dto.EntregaResponse;
import com.ensora.evento.CategoriaEvento;
import com.ensora.evento.EventoService;
import com.ensora.evento.dto.EventoResponse;
import com.ensora.habito.HabitoDoDia;
import com.ensora.habito.HabitoService;
import com.ensora.habito.TipoHabito;
import com.ensora.inicio.dto.InicioResponse;
import com.ensora.inicio.dto.InicioResponse.HabitoNaHome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicioServiceTest {

	private static final Long USUARIO = 7L;
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Mock
	private HabitoService habitoService;

	@Mock
	private EntregaService entregaService;

	@Mock
	private EventoService eventoService;

	private InicioService servico() {
		return new InicioService(habitoService, entregaService, eventoService);
	}

	@Test
	void carregarDevolveADataOsHabitosDoDiaEOUpcoming() {
		EntregaResponse entrega = new EntregaResponse(3L, "Trabalho", HOJE.plusDays(1), null, false, null, null, null);
		EventoResponse evento = new EventoResponse(8L, "Consulta", HOJE.plusDays(2), LocalTime.of(9, 0), null, null,
				CategoriaEvento.SAUDE);
		when(habitoService.listarDoDia(USUARIO, HOJE)).thenReturn(List.of(
				new HabitoDoDia(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoDoDia(2L, "Academia", TipoHabito.SEMANAL, false, 1, 3)));
		when(entregaService.listarParaHome(USUARIO, HOJE.minusDays(1), HOJE.plusDays(4))).thenReturn(List.of(entrega));
		when(eventoService.listarNoIntervalo(USUARIO, HOJE, HOJE.plusDays(4))).thenReturn(List.of(evento));

		InicioResponse resposta = servico().carregar(USUARIO, HOJE);

		assertThat(resposta.data()).isEqualTo(HOJE);
		assertThat(resposta.habitos()).containsExactly(
				new HabitoNaHome(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoNaHome(2L, "Academia", TipoHabito.SEMANAL, false, 1, 3));
		assertThat(resposta.upcoming().entregas()).containsExactly(entrega);
		assertThat(resposta.upcoming().eventos()).containsExactly(evento);
	}

	@Test
	void carregarPedeAsEntregasDesdeOntemEOsEventosDeHojeAteQuatroDiasDepois() {
		when(habitoService.listarDoDia(USUARIO, HOJE)).thenReturn(List.of());
		when(entregaService.listarParaHome(USUARIO, HOJE.minusDays(1), HOJE.plusDays(4))).thenReturn(List.of());
		when(eventoService.listarNoIntervalo(USUARIO, HOJE, HOJE.plusDays(4))).thenReturn(List.of());

		InicioResponse resposta = servico().carregar(USUARIO, HOJE);

		verify(habitoService).listarDoDia(USUARIO, HOJE);
		verify(entregaService).listarParaHome(USUARIO, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 25));
		verify(eventoService).listarNoIntervalo(USUARIO, HOJE, LocalDate.of(2026, 10, 25));
		assertThat(resposta.habitos()).isEmpty();
		assertThat(resposta.upcoming().entregas()).isEmpty();
		assertThat(resposta.upcoming().eventos()).isEmpty();
	}

	@Test
	void carregarComDataInvalidaPropagaOErroDosHabitosSemConsultarEntregasNemEventos() {
		when(habitoService.listarDoDia(USUARIO, HOJE.minusDays(30)))
				.thenThrow(new ValidacaoException("data", "muito distante da data de hoje"));

		assertThatThrownBy(() -> servico().carregar(USUARIO, HOJE.minusDays(30)))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));

		verifyNoInteractions(entregaService, eventoService);
	}

}
