package com.ensora.evento;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.evento.dto.EventoRequest;
import com.ensora.evento.dto.EventoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

	private static final Long USUARIO = 7L;
	private static final Long ID = 10L;
	private static final LocalDate DATA = LocalDate.of(2026, 11, 5);
	private static final LocalTime NOVE = LocalTime.of(9, 0);
	private static final LocalTime DEZ = LocalTime.of(10, 0);

	@Mock
	private EventoRepository eventos;

	private EventoService servico() {
		return new EventoService(eventos);
	}

	private Evento evento() {
		Evento evento = new Evento();
		evento.setId(ID);
		evento.setUsuarioId(USUARIO);
		evento.setTitulo("Consulta");
		evento.setData(DATA);
		evento.setInicio(NOVE);
		evento.setTermino(DEZ);
		evento.setLocal("Clínica");
		evento.setCategoria(CategoriaEvento.SAUDE);
		return evento;
	}

	private Evento acharEvento(Evento evento) {
		when(eventos.findByIdAndUsuarioId(ID, USUARIO)).thenReturn(Optional.of(evento));
		return evento;
	}

	private void naoAcharEvento() {
		// De outro usuário ou inexistente: o Repository devolve vazio nos dois casos.
		when(eventos.findByIdAndUsuarioId(ID, USUARIO)).thenReturn(Optional.empty());
	}

	private void assertTermino(Runnable acao) {
		assertThatThrownBy(acao::run)
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("termino"));
	}

	// ---- criar ----

	@Test
	void criarGravaComOUsuarioDoTokenEApagaOsEspacosDeTituloELocal() {
		when(eventos.save(any(Evento.class))).thenAnswer(invocacao -> {
			Evento salvo = invocacao.getArgument(0);
			salvo.setId(ID);
			return salvo;
		});

		EventoResponse resposta = servico().criar(USUARIO,
				new EventoRequest("  Consulta  ", DATA, NOVE, DEZ, "  Clínica  ", CategoriaEvento.SAUDE));

		ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
		verify(eventos).save(captor.capture());
		Evento gravado = captor.getValue();
		assertThat(gravado.getUsuarioId()).isEqualTo(USUARIO);
		assertThat(gravado.getTitulo()).isEqualTo("Consulta");
		assertThat(gravado.getData()).isEqualTo(DATA);
		assertThat(gravado.getInicio()).isEqualTo(NOVE);
		assertThat(gravado.getTermino()).isEqualTo(DEZ);
		assertThat(gravado.getLocal()).isEqualTo("Clínica");
		assertThat(gravado.getCategoria()).isEqualTo(CategoriaEvento.SAUDE);
		assertThat(resposta).isEqualTo(
				new EventoResponse(ID, "Consulta", DATA, NOVE, DEZ, "Clínica", CategoriaEvento.SAUDE));
	}

	@Test
	void criarSemTerminoLocalECategoriaGravaNulos() {
		when(eventos.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico().criar(USUARIO, new EventoRequest("Reunião", DATA, NOVE, null, null, null));

		ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
		verify(eventos).save(captor.capture());
		assertThat(captor.getValue().getTermino()).isNull();
		assertThat(captor.getValue().getLocal()).isNull();
		assertThat(captor.getValue().getCategoria()).isNull();
	}

	@Test
	void criarComLocalSoDeEspacosGravaLocalAusente() {
		when(eventos.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico().criar(USUARIO, new EventoRequest("Reunião", DATA, NOVE, null, "   ", null));

		ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
		verify(eventos).save(captor.capture());
		assertThat(captor.getValue().getLocal()).isNull();
	}

	@Test
	void criarComTerminoIgualAoInicioLancaValidacaoNoCampoTerminoSemGravar() {
		assertTermino(() -> servico().criar(USUARIO, new EventoRequest("X", DATA, NOVE, NOVE, null, null)));

		verify(eventos, never()).save(any());
	}

	@Test
	void criarComTerminoAntesDoInicioLancaValidacaoNoCampoTerminoSemGravar() {
		assertTermino(() -> servico().criar(USUARIO, new EventoRequest("X", DATA, DEZ, NOVE, null, null)));

		verify(eventos, never()).save(any());
	}

	@Test
	void criarAceitaTerminoUmMinutoDepoisDoInicio() {
		when(eventos.save(any(Evento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico().criar(USUARIO, new EventoRequest("X", DATA, NOVE, NOVE.plusMinutes(1), null, null));

		verify(eventos).save(any(Evento.class));
	}

	// ---- atualizar ----

	@Test
	void atualizarTrocaTodosOsCampos() {
		Evento evento = acharEvento(evento());
		LocalDate novaData = LocalDate.of(2026, 11, 12);

		EventoResponse resposta = servico().atualizar(USUARIO, ID, new EventoRequest(" Reunião ", novaData,
				LocalTime.of(14, 0), LocalTime.of(15, 30), " Sala 2 ", CategoriaEvento.TRABALHO));

		assertThat(evento.getTitulo()).isEqualTo("Reunião");
		assertThat(evento.getData()).isEqualTo(novaData);
		assertThat(evento.getInicio()).isEqualTo(LocalTime.of(14, 0));
		assertThat(evento.getTermino()).isEqualTo(LocalTime.of(15, 30));
		assertThat(evento.getLocal()).isEqualTo("Sala 2");
		assertThat(evento.getCategoria()).isEqualTo(CategoriaEvento.TRABALHO);
		assertThat(resposta.titulo()).isEqualTo("Reunião");
		verify(eventos).findByIdAndUsuarioId(ID, USUARIO);
	}

	@Test
	void atualizarLimpaTerminoLocalECategoriaQueVieremAusentes() {
		Evento evento = acharEvento(evento());

		EventoResponse resposta = servico().atualizar(USUARIO, ID,
				new EventoRequest("Consulta", DATA, NOVE, null, null, null));

		assertThat(evento.getTermino()).isNull();
		assertThat(evento.getLocal()).isNull();
		assertThat(evento.getCategoria()).isNull();
		assertThat(resposta.termino()).isNull();
		assertThat(resposta.local()).isNull();
		assertThat(resposta.categoria()).isNull();
	}

	@Test
	void atualizarComTerminoAntesDoInicioLancaValidacaoSemAlterarOEvento() {
		Evento evento = acharEvento(evento());

		assertTermino(() -> servico().atualizar(USUARIO, ID,
				new EventoRequest("Outro título", DATA, DEZ, NOVE, null, null)));

		assertThat(evento.getTitulo()).isEqualTo("Consulta");
		assertThat(evento.getInicio()).isEqualTo(NOVE);
	}

	@Test
	void atualizarEventoQueOFiltroNaoAchaLancaNaoEncontradoSemGravar() {
		naoAcharEvento();

		assertThatThrownBy(() -> servico().atualizar(USUARIO, ID, new EventoRequest("X", DATA, NOVE, null, null, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(eventos).findByIdAndUsuarioId(ID, USUARIO);
		verify(eventos, never()).save(any());
	}

	// ---- Home ----

	@Test
	void listarNoIntervaloConsultaSoOsEventosDoUsuarioNoIntervaloRecebidoEMapeiaParaResposta() {
		LocalDate ate = DATA.plusDays(4);
		Evento primeiro = evento();
		Evento segundo = evento();
		segundo.setId(11L);
		segundo.setTermino(null);
		segundo.setLocal(null);
		segundo.setCategoria(null);
		when(eventos.findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(USUARIO, DATA, ate))
				.thenReturn(List.of(primeiro, segundo));

		List<EventoResponse> lista = servico().listarNoIntervalo(USUARIO, DATA, ate);

		assertThat(lista).extracting(EventoResponse::id).containsExactly(ID, 11L);
		assertThat(lista.get(0).categoria()).isEqualTo(CategoriaEvento.SAUDE);
		assertThat(lista.get(1).categoria()).isNull();
		verify(eventos).findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(USUARIO, DATA, ate);
	}

	@Test
	void listarNoIntervaloSemEventosDevolveListaVazia() {
		when(eventos.findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(USUARIO, DATA, DATA))
				.thenReturn(List.of());

		assertThat(servico().listarNoIntervalo(USUARIO, DATA, DATA)).isEmpty();
	}

	// ---- excluir ----

	@Test
	void excluirApagaOEventoDoUsuario() {
		Evento evento = acharEvento(evento());

		servico().excluir(USUARIO, ID);

		verify(eventos).findByIdAndUsuarioId(ID, USUARIO);
		verify(eventos).delete(evento);
	}

	@Test
	void excluirEventoQueOFiltroNaoAchaLancaNaoEncontradoSemApagarNada() {
		naoAcharEvento();

		assertThatThrownBy(() -> servico().excluir(USUARIO, ID))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(eventos).findByIdAndUsuarioId(ID, USUARIO);
		verify(eventos, never()).delete(any());
	}

}
