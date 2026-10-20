package com.ensora.entrega;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.entrega.dto.EntregaRequest;
import com.ensora.entrega.dto.EntregaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

	private static final Long USUARIO = 7L;
	private static final Long ID = 10L;
	private static final LocalDate PRAZO = LocalDate.of(2026, 11, 5);
	private static final Instant AGORA = LocalDate.of(2026, 10, 21).atTime(12, 0).toInstant(ZoneOffset.UTC);

	@Mock
	private EntregaRepository entregas;

	private EntregaService servico() {
		return new EntregaService(entregas, Clock.fixed(AGORA, ZoneOffset.UTC));
	}

	private Entrega entrega() {
		Entrega entrega = new Entrega();
		entrega.setId(ID);
		entrega.setUsuarioId(USUARIO);
		entrega.setTitulo("Trabalho");
		entrega.setPrazoData(PRAZO);
		return entrega;
	}

	private Entrega acharEntrega(Entrega entrega) {
		when(entregas.findByIdAndUsuarioId(ID, USUARIO)).thenReturn(Optional.of(entrega));
		return entrega;
	}

	private void naoAcharEntrega() {
		// De outro usuário ou inexistente: o Repository devolve vazio nos dois casos.
		when(entregas.findByIdAndUsuarioId(ID, USUARIO)).thenReturn(Optional.empty());
	}

	// ---- criar ----

	@Test
	void criarGravaComOUsuarioDoTokenEApagaOsEspacosDoTitulo() {
		when(entregas.save(any(Entrega.class))).thenAnswer(invocacao -> {
			Entrega salva = invocacao.getArgument(0);
			salva.setId(ID);
			return salva;
		});

		EntregaResponse resposta = servico().criar(USUARIO,
				new EntregaRequest("  Trabalho de Cálculo  ", PRAZO, LocalTime.of(14, 30), "Cap. 1 a 4"));

		ArgumentCaptor<Entrega> captor = ArgumentCaptor.forClass(Entrega.class);
		verify(entregas).save(captor.capture());
		Entrega gravada = captor.getValue();
		assertThat(gravada.getUsuarioId()).isEqualTo(USUARIO);
		assertThat(gravada.getTitulo()).isEqualTo("Trabalho de Cálculo");
		assertThat(gravada.getPrazoData()).isEqualTo(PRAZO);
		assertThat(gravada.getPrazoHorario()).isEqualTo(LocalTime.of(14, 30));
		assertThat(gravada.getNota()).isEqualTo("Cap. 1 a 4");
		assertThat(gravada.isConcluida()).isFalse();
		assertThat(gravada.getConcluidaEm()).isNull();
		assertThat(resposta).isEqualTo(
				new EntregaResponse(ID, "Trabalho de Cálculo", PRAZO, LocalTime.of(14, 30), false, null, "Cap. 1 a 4"));
	}

	@Test
	void criarSemHorarioENotaGravaNulos() {
		when(entregas.save(any(Entrega.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico().criar(USUARIO, new EntregaRequest("Prova", PRAZO, null, null));

		ArgumentCaptor<Entrega> captor = ArgumentCaptor.forClass(Entrega.class);
		verify(entregas).save(captor.capture());
		assertThat(captor.getValue().getPrazoHorario()).isNull();
		assertThat(captor.getValue().getNota()).isNull();
	}

	@Test
	void criarComNotaSoDeEspacosGravaNotaAusente() {
		when(entregas.save(any(Entrega.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		servico().criar(USUARIO, new EntregaRequest("Prova", PRAZO, null, "   "));

		ArgumentCaptor<Entrega> captor = ArgumentCaptor.forClass(Entrega.class);
		verify(entregas).save(captor.capture());
		assertThat(captor.getValue().getNota()).isNull();
	}

	// ---- atualizar ----

	@Test
	void atualizarTrocaTituloPrazoHorarioENota() {
		Entrega entrega = acharEntrega(entrega());
		LocalDate novoPrazo = LocalDate.of(2026, 11, 12);

		EntregaResponse resposta = servico().atualizar(USUARIO, ID,
				new EntregaRequest(" Novo título ", novoPrazo, LocalTime.of(9, 0), "Nova nota"));

		assertThat(entrega.getTitulo()).isEqualTo("Novo título");
		assertThat(entrega.getPrazoData()).isEqualTo(novoPrazo);
		assertThat(entrega.getPrazoHorario()).isEqualTo(LocalTime.of(9, 0));
		assertThat(entrega.getNota()).isEqualTo("Nova nota");
		assertThat(resposta.titulo()).isEqualTo("Novo título");
		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
	}

	@Test
	void atualizarNaoMexeNaConclusao() {
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(AGORA.minusSeconds(3600));
		acharEntrega(entrega);

		EntregaResponse resposta = servico().atualizar(USUARIO, ID,
				new EntregaRequest("Outro título", PRAZO, null, null));

		assertThat(entrega.isConcluida()).isTrue();
		assertThat(entrega.getConcluidaEm()).isEqualTo(AGORA.minusSeconds(3600));
		assertThat(resposta.concluida()).isTrue();
	}

	@Test
	void atualizarEntregaQueOFiltroNaoAchaLancaNaoEncontradoSemGravar() {
		naoAcharEntrega();

		assertThatThrownBy(() -> servico().atualizar(USUARIO, ID, new EntregaRequest("X", PRAZO, null, null)))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
		verify(entregas, never()).save(any());
	}

	// ---- conclusão ----

	@Test
	void concluirDefineConcluidaEConcluidaEmComOInstanteDoRelogio() {
		Entrega entrega = acharEntrega(entrega());

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, true);

		assertThat(entrega.isConcluida()).isTrue();
		assertThat(entrega.getConcluidaEm()).isEqualTo(AGORA);
		assertThat(resposta.concluida()).isTrue();
		assertThat(resposta.concluidaEm()).isEqualTo(AGORA);
	}

	@Test
	void concluirDeNovoMantemAHoraOriginal() {
		Instant antes = AGORA.minusSeconds(86_400);
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(antes);
		acharEntrega(entrega);

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, true);

		assertThat(entrega.getConcluidaEm()).isEqualTo(antes);
		assertThat(resposta.concluidaEm()).isEqualTo(antes);
	}

	@Test
	void desfazerLimpaConcluidaEConcluidaEm() {
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(AGORA.minusSeconds(60));
		acharEntrega(entrega);

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, false);

		assertThat(entrega.isConcluida()).isFalse();
		assertThat(entrega.getConcluidaEm()).isNull();
		assertThat(resposta.concluida()).isFalse();
		assertThat(resposta.concluidaEm()).isNull();
	}

	@Test
	void desfazerOQueNaoEstaConcluidoNaoFazNada() {
		Entrega entrega = acharEntrega(entrega());

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, false);

		assertThat(entrega.isConcluida()).isFalse();
		assertThat(entrega.getConcluidaEm()).isNull();
		assertThat(resposta.concluida()).isFalse();
	}

	@Test
	void alterarConclusaoDeEntregaQueOFiltroNaoAchaLancaNaoEncontrado() {
		naoAcharEntrega();

		assertThatThrownBy(() -> servico().alterarConclusao(USUARIO, ID, true))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
	}

	// ---- excluir ----

	@Test
	void excluirApagaAEntregaDoUsuario() {
		Entrega entrega = acharEntrega(entrega());

		servico().excluir(USUARIO, ID);

		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
		verify(entregas).delete(entrega);
	}

	@Test
	void excluirEntregaQueOFiltroNaoAchaLancaNaoEncontradoSemApagarNada() {
		naoAcharEntrega();

		assertThatThrownBy(() -> servico().excluir(USUARIO, ID))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
		verify(entregas, never()).delete(any());
	}

}
