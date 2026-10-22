package com.ensora.entrega;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaServiceTest {

	private static final Long USUARIO = 7L;
	private static final Long ID = 10L;
	private static final LocalDate PRAZO = LocalDate.of(2026, 11, 5);
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
	private static final Instant AGORA = HOJE.atTime(12, 0).toInstant(ZoneOffset.UTC);

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
				new EntregaResponse(ID, "Trabalho de Cálculo", PRAZO, LocalTime.of(14, 30), false, null, null,
						"Cap. 1 a 4"));
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
	void concluirDefineConcluidaConcluidaEmDoRelogioEConcluidaDataEnviada() {
		Entrega entrega = acharEntrega(entrega());

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, true, HOJE);

		assertThat(entrega.isConcluida()).isTrue();
		assertThat(entrega.getConcluidaEm()).isEqualTo(AGORA);
		assertThat(entrega.getConcluidaData()).isEqualTo(HOJE);
		assertThat(resposta.concluida()).isTrue();
		assertThat(resposta.concluidaEm()).isEqualTo(AGORA);
		assertThat(resposta.concluidaData()).isEqualTo(HOJE);
	}

	@Test
	void concluirComADataDeOntemDoFusoDoUsuarioGravaEssaData() {
		// O instante do servidor é 21/10 às 12h UTC, mas no fuso do usuário ainda pode ser 20/10: vale o que ele enviou.
		Entrega entrega = acharEntrega(entrega());

		servico().alterarConclusao(USUARIO, ID, true, HOJE.minusDays(1));

		assertThat(entrega.getConcluidaData()).isEqualTo(HOJE.minusDays(1));
	}

	@Test
	void concluirDeNovoMantemAHoraEADataOriginais() {
		Instant antes = AGORA.minusSeconds(86_400);
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(antes);
		entrega.setConcluidaData(HOJE.minusDays(1));
		acharEntrega(entrega);

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, true, HOJE);

		assertThat(entrega.getConcluidaEm()).isEqualTo(antes);
		assertThat(entrega.getConcluidaData()).isEqualTo(HOJE.minusDays(1));
		assertThat(resposta.concluidaEm()).isEqualTo(antes);
		assertThat(resposta.concluidaData()).isEqualTo(HOJE.minusDays(1));
	}

	@Test
	void concluirSemDataLancaValidacaoNoCampoDataSemAlterarAEntrega() {
		Entrega entrega = entrega();

		assertThatThrownBy(() -> servico().alterarConclusao(USUARIO, ID, true, null))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));

		assertThat(entrega.isConcluida()).isFalse();
		verifyNoInteractions(entregas);
	}

	@Test
	void concluirComDataAMaisDeDoisDiasDoRelogioLancaValidacaoNoCampoData() {
		assertThatThrownBy(() -> servico().alterarConclusao(USUARIO, ID, true, HOJE.plusDays(3)))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));
		assertThatThrownBy(() -> servico().alterarConclusao(USUARIO, ID, true, HOJE.minusDays(3)))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));

		verifyNoInteractions(entregas);
	}

	@Test
	void concluirAceitaDataAteDoisDiasDeDiferenca() {
		Entrega entrega = acharEntrega(entrega());

		servico().alterarConclusao(USUARIO, ID, true, HOJE.plusDays(2));

		assertThat(entrega.getConcluidaData()).isEqualTo(HOJE.plusDays(2));
	}

	@Test
	void desfazerLimpaConcluidaConcluidaEmEConcluidaData() {
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(AGORA.minusSeconds(60));
		entrega.setConcluidaData(HOJE);
		acharEntrega(entrega);

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, false, null);

		assertThat(entrega.isConcluida()).isFalse();
		assertThat(entrega.getConcluidaEm()).isNull();
		assertThat(entrega.getConcluidaData()).isNull();
		assertThat(resposta.concluida()).isFalse();
		assertThat(resposta.concluidaEm()).isNull();
		assertThat(resposta.concluidaData()).isNull();
	}

	@Test
	void desfazerIgnoraADataEnviada() {
		Entrega entrega = entrega();
		entrega.setConcluida(true);
		entrega.setConcluidaEm(AGORA.minusSeconds(60));
		entrega.setConcluidaData(HOJE);
		acharEntrega(entrega);

		// Mesmo uma data absurda não atrapalha: ao desfazer ela não é lida.
		servico().alterarConclusao(USUARIO, ID, false, HOJE.plusDays(30));

		assertThat(entrega.isConcluida()).isFalse();
	}

	@Test
	void desfazerOQueNaoEstaConcluidoNaoFazNada() {
		Entrega entrega = acharEntrega(entrega());

		EntregaResponse resposta = servico().alterarConclusao(USUARIO, ID, false, null);

		assertThat(entrega.isConcluida()).isFalse();
		assertThat(entrega.getConcluidaEm()).isNull();
		assertThat(entrega.getConcluidaData()).isNull();
		assertThat(resposta.concluida()).isFalse();
	}

	@Test
	void alterarConclusaoDeEntregaQueOFiltroNaoAchaLancaNaoEncontrado() {
		naoAcharEntrega();

		assertThatThrownBy(() -> servico().alterarConclusao(USUARIO, ID, true, HOJE))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(entregas).findByIdAndUsuarioId(ID, USUARIO);
	}

	// ---- Calendar ----

	@Test
	void listarNoIntervaloConsultaSoAsEntregasDoUsuarioNoIntervaloEIncluiAsConcluidas() {
		LocalDate de = LocalDate.of(2026, 10, 25);
		LocalDate ate = LocalDate.of(2026, 12, 5);
		Entrega pendente = entrega();
		Entrega concluida = entrega();
		concluida.setId(11L);
		concluida.setConcluida(true);
		concluida.setConcluidaEm(AGORA);
		concluida.setConcluidaData(HOJE);
		when(entregas.findNoIntervalo(USUARIO, de, ate)).thenReturn(List.of(pendente, concluida));

		List<EntregaResponse> lista = servico().listarNoIntervalo(USUARIO, de, ate);

		assertThat(lista).extracting(EntregaResponse::id).containsExactly(ID, 11L);
		assertThat(lista).extracting(EntregaResponse::concluida).containsExactly(false, true);
		verify(entregas).findNoIntervalo(USUARIO, de, ate);
	}

	@Test
	void listarNoIntervaloSemEntregasDevolveListaVazia() {
		when(entregas.findNoIntervalo(USUARIO, HOJE, HOJE)).thenReturn(List.of());

		assertThat(servico().listarNoIntervalo(USUARIO, HOJE, HOJE)).isEmpty();
	}

	// ---- Home ----

	@Test
	void listarParaHomeConsultaSoAsEntregasDoUsuarioNaJanelaRecebidaEMapeiaParaResposta() {
		LocalDate ontem = HOJE.minusDays(1);
		LocalDate ate = HOJE.plusDays(4);
		Entrega pendente = entrega();
		Entrega concluida = entrega();
		concluida.setId(11L);
		concluida.setConcluida(true);
		concluida.setConcluidaEm(AGORA);
		concluida.setConcluidaData(HOJE);
		when(entregas.findParaHome(USUARIO, ontem, ate)).thenReturn(List.of(pendente, concluida));

		List<EntregaResponse> lista = servico().listarParaHome(USUARIO, ontem, ate);

		assertThat(lista).extracting(EntregaResponse::id).containsExactly(ID, 11L);
		assertThat(lista.get(1).concluida()).isTrue();
		assertThat(lista.get(1).concluidaData()).isEqualTo(HOJE);
		verify(entregas).findParaHome(USUARIO, ontem, ate);
	}

	@Test
	void listarParaHomeSemEntregasDevolveListaVazia() {
		when(entregas.findParaHome(USUARIO, HOJE, HOJE)).thenReturn(List.of());

		assertThat(servico().listarParaHome(USUARIO, HOJE, HOJE)).isEmpty();
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

	@Test
	void apagarTodasDoUsuarioApagaSoAsEntregasDele() {
		servico().apagarTodasDoUsuario(7L);

		verify(entregas).apagarDoUsuario(7L);
	}

}
