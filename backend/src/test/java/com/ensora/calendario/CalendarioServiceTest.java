package com.ensora.calendario;

import com.ensora.calendario.dto.CalendarioResponse;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.entrega.EntregaService;
import com.ensora.entrega.dto.EntregaResponse;
import com.ensora.evento.CategoriaEvento;
import com.ensora.evento.EventoService;
import com.ensora.evento.dto.EventoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioServiceTest {

	private static final Long USUARIO = 7L;
	private static final LocalDate DE = LocalDate.of(2026, 10, 25);
	private static final LocalDate ATE = LocalDate.of(2026, 12, 5);

	@Mock
	private EntregaService entregaService;

	@Mock
	private EventoService eventoService;

	private CalendarioService servico() {
		return new CalendarioService(entregaService, eventoService);
	}

	private void assertErroNoCampoAte(LocalDate de, LocalDate ate) {
		assertThatThrownBy(() -> servico().carregar(USUARIO, de, ate))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("ate"));
		verifyNoInteractions(entregaService, eventoService);
	}

	@Test
	void carregarDevolveOIntervaloEAsEntregasEEventosDoUsuario() {
		EntregaResponse entrega = new EntregaResponse(3L, "Trabalho", LocalDate.of(2026, 11, 5), null, false, null,
				null, null);
		EventoResponse evento = new EventoResponse(8L, "Consulta", LocalDate.of(2026, 11, 5), LocalTime.of(9, 0), null,
				null, CategoriaEvento.SAUDE);
		when(entregaService.listarNoIntervalo(USUARIO, DE, ATE)).thenReturn(List.of(entrega));
		when(eventoService.listarNoIntervalo(USUARIO, DE, ATE)).thenReturn(List.of(evento));

		CalendarioResponse resposta = servico().carregar(USUARIO, DE, ATE);

		assertThat(resposta.de()).isEqualTo(DE);
		assertThat(resposta.ate()).isEqualTo(ATE);
		assertThat(resposta.entregas()).containsExactly(entrega);
		assertThat(resposta.eventos()).containsExactly(evento);
		verify(entregaService).listarNoIntervalo(USUARIO, DE, ATE);
		verify(eventoService).listarNoIntervalo(USUARIO, DE, ATE);
	}

	@Test
	void carregarSemNadaDevolveListasVazias() {
		when(entregaService.listarNoIntervalo(USUARIO, DE, ATE)).thenReturn(List.of());
		when(eventoService.listarNoIntervalo(USUARIO, DE, ATE)).thenReturn(List.of());

		CalendarioResponse resposta = servico().carregar(USUARIO, DE, ATE);

		assertThat(resposta.entregas()).isEmpty();
		assertThat(resposta.eventos()).isEmpty();
	}

	@Test
	void carregarAceitaIntervaloDeUmDiaSo() {
		when(entregaService.listarNoIntervalo(USUARIO, DE, DE)).thenReturn(List.of());
		when(eventoService.listarNoIntervalo(USUARIO, DE, DE)).thenReturn(List.of());

		CalendarioResponse resposta = servico().carregar(USUARIO, DE, DE);

		assertThat(resposta.de()).isEqualTo(DE);
		assertThat(resposta.ate()).isEqualTo(DE);
	}

	@Test
	void carregarAceitaIntervaloDeAte62DiasEntreAsPontas() {
		LocalDate ate = DE.plusDays(62);
		when(entregaService.listarNoIntervalo(USUARIO, DE, ate)).thenReturn(List.of());
		when(eventoService.listarNoIntervalo(USUARIO, DE, ate)).thenReturn(List.of());

		servico().carregar(USUARIO, DE, ate);

		verify(entregaService).listarNoIntervalo(USUARIO, DE, ate);
	}

	@Test
	void carregarComIntervaloDe63DiasLancaValidacaoNoCampoAteSemConsultar() {
		assertErroNoCampoAte(DE, DE.plusDays(63));
	}

	@Test
	void carregarComAteAntesDeDeLancaValidacaoNoCampoAteSemConsultar() {
		assertErroNoCampoAte(DE, DE.minusDays(1));
	}

	@Test
	void carregarAceitaIntervaloNoPassadoDistanteSemRegraDeHoje() {
		LocalDate de = LocalDate.of(2020, 1, 1);
		LocalDate ate = LocalDate.of(2020, 1, 31);
		when(entregaService.listarNoIntervalo(USUARIO, de, ate)).thenReturn(List.of());
		when(eventoService.listarNoIntervalo(USUARIO, de, ate)).thenReturn(List.of());

		CalendarioResponse resposta = servico().carregar(USUARIO, de, ate);

		assertThat(resposta.entregas()).isEmpty();
		verify(eventoService).listarNoIntervalo(any(), any(), any());
	}

}
