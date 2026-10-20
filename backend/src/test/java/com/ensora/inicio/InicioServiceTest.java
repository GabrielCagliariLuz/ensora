package com.ensora.inicio;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InicioServiceTest {

	private static final Long USUARIO = 7L;
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Mock
	private HabitoService habitoService;

	@Test
	void carregarDevolveADataEOsHabitosDoDiaDoUsuario() {
		when(habitoService.listarDoDia(USUARIO, HOJE)).thenReturn(List.of(
				new HabitoDoDia(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoDoDia(2L, "Academia", TipoHabito.SEMANAL, false, 1, 3)));

		InicioResponse resposta = new InicioService(habitoService).carregar(USUARIO, HOJE);

		assertThat(resposta.data()).isEqualTo(HOJE);
		assertThat(resposta.habitos()).containsExactly(
				new HabitoNaHome(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoNaHome(2L, "Academia", TipoHabito.SEMANAL, false, 1, 3));
		verify(habitoService).listarDoDia(USUARIO, HOJE);
	}

}
