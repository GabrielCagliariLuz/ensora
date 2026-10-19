package com.ensora.habito;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroHabitoServiceTest {

	private static final Long USUARIO = 7L;
	private static final Long HABITO = 10L;
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Mock
	private HabitoRepository habitos;

	@Mock
	private RegistroHabitoRepository registros;

	private RegistroHabitoService servico() {
		Clock relogio = Clock.fixed(HOJE.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		return new RegistroHabitoService(habitos, registros, relogio);
	}

	private void acharHabito() {
		Habito habito = new Habito();
		habito.setId(HABITO);
		habito.setUsuarioId(USUARIO);
		when(habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(HABITO, USUARIO)).thenReturn(Optional.of(habito));
	}

	private void naoAcharHabito() {
		// De outro usuário, excluído ou inexistente: o Repository devolve vazio nos três casos.
		when(habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(HABITO, USUARIO)).thenReturn(Optional.empty());
	}

	@Test
	void marcarCriaORegistroDoHabitoNaData() {
		acharHabito();
		when(registros.marcar(HABITO, HOJE)).thenReturn(1);

		servico().marcar(USUARIO, HABITO, HOJE);

		verify(registros).marcar(HABITO, HOJE);
	}

	@Test
	void marcarDuasVezesNaoDaErroEDeixaAUnicidadeParaOBanco() {
		acharHabito();
		// A 1ª chamada cria (1 linha); a 2ª encontra o registro e o banco não faz nada (0 linhas).
		when(registros.marcar(HABITO, HOJE)).thenReturn(1, 0);
		RegistroHabitoService servico = servico();

		assertThatCode(() -> {
			servico.marcar(USUARIO, HABITO, HOJE);
			servico.marcar(USUARIO, HABITO, HOJE);
		}).doesNotThrowAnyException();

		// Nunca insere por outro caminho: só pelo INSERT ... ON CONFLICT DO NOTHING.
		verify(registros, times(2)).marcar(HABITO, HOJE);
		verify(registros, never()).save(any());
	}

	@Test
	void marcarHabitoQueOFiltroNaoAchaLancaNaoEncontradoSemGravar() {
		naoAcharHabito();

		assertThatThrownBy(() -> servico().marcar(USUARIO, HABITO, HOJE))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(habitos).findByIdAndUsuarioIdAndExcluidoEmIsNull(HABITO, USUARIO);
		verifyNoInteractions(registros);
	}

	@Test
	void marcarDataFuturaAlemDaToleranciaLancaValidacaoNoCampoData() {
		assertThatThrownBy(() -> servico().marcar(USUARIO, HABITO, HOJE.plusDays(3)))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));

		verifyNoInteractions(registros);
	}

	@Test
	void marcarAceitaDataAteDoisDiasNoFuturo() {
		acharHabito();

		assertThatCode(() -> servico().marcar(USUARIO, HABITO, HOJE.plusDays(2))).doesNotThrowAnyException();

		verify(registros).marcar(HABITO, HOJE.plusDays(2));
	}

	@Test
	void marcarAceitaDataNoPassadoSemLimite() {
		acharHabito();
		LocalDate haUmAno = HOJE.minusYears(1);

		assertThatCode(() -> servico().marcar(USUARIO, HABITO, haUmAno)).doesNotThrowAnyException();

		verify(registros).marcar(HABITO, haUmAno);
	}

	@Test
	void desmarcarRemoveORegistroDaData() {
		acharHabito();
		when(registros.desmarcar(HABITO, HOJE)).thenReturn(1);

		servico().desmarcar(USUARIO, HABITO, HOJE);

		verify(registros).desmarcar(HABITO, HOJE);
	}

	@Test
	void desmarcarOQueNaoEstaMarcadoNaoDaErro() {
		acharHabito();
		when(registros.desmarcar(HABITO, HOJE)).thenReturn(0);

		assertThatCode(() -> servico().desmarcar(USUARIO, HABITO, HOJE)).doesNotThrowAnyException();

		verify(registros).desmarcar(HABITO, HOJE);
	}

	@Test
	void desmarcarHabitoQueOFiltroNaoAchaLancaNaoEncontradoSemApagarNada() {
		naoAcharHabito();

		assertThatThrownBy(() -> servico().desmarcar(USUARIO, HABITO, HOJE))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(habitos).findByIdAndUsuarioIdAndExcluidoEmIsNull(HABITO, USUARIO);
		verifyNoInteractions(registros);
	}

}
