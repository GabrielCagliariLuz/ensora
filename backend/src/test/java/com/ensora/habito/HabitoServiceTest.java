package com.ensora.habito;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.habito.dto.HabitoRequest;
import com.ensora.habito.dto.HabitoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class HabitoServiceTest {

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
	private static final Long USUARIO = 7L;
	// 21/10/2026 é quarta-feira; a segunda da semana é 19/10/2026.
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Mock
	private HabitoRepository habitos;

	@Mock
	private HabitoMetaRepository metas;

	@Mock
	private RegistroHabitoRepository registros;

	private HabitoService servico(LocalDate hoje) {
		Clock relogio = Clock.fixed(hoje.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		return new HabitoService(habitos, metas, registros, relogio);
	}

	private HabitoService servico() {
		return servico(HOJE);
	}

	private void salvarHabitoComId(Long id) {
		when(habitos.save(any(Habito.class))).thenAnswer(invocacao -> {
			Habito salvo = invocacao.getArgument(0);
			salvo.setId(id);
			return salvo;
		});
	}

	private Habito habito(Long id, String nome, TipoHabito tipo) {
		Habito habito = new Habito();
		habito.setId(id);
		habito.setUsuarioId(USUARIO);
		habito.setNome(nome);
		habito.setTipo(tipo);
		return habito;
	}

	private HabitoMeta meta(Long habitoId, int vezes, LocalDate vigenteDesde) {
		HabitoMeta meta = new HabitoMeta();
		meta.setHabitoId(habitoId);
		meta.setVezesPorSemana(vezes);
		meta.setVigenteDesde(vigenteDesde);
		return meta;
	}

	private void assertValidacao(Runnable acao, String campo) {
		assertThatThrownBy(acao::run)
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo(campo));
		verify(habitos, never()).save(any());
		verify(metas, never()).save(any());
	}

	@Test
	void criarHabitoDiarioSalvaOHabitoENaoCriaMeta() {
		salvarHabitoComId(10L);

		HabitoResponse resposta = servico().criar(USUARIO, new HabitoRequest("Beber água", TipoHabito.DIARIO, null, HOJE));

		assertThat(resposta.id()).isEqualTo(10L);
		assertThat(resposta.tipo()).isEqualTo(TipoHabito.DIARIO);
		assertThat(resposta.vezesPorSemana()).isNull();
		ArgumentCaptor<Habito> salvo = ArgumentCaptor.forClass(Habito.class);
		verify(habitos).save(salvo.capture());
		assertThat(salvo.getValue().getUsuarioId()).isEqualTo(USUARIO);
		verifyNoInteractions(metas);
	}

	@ParameterizedTest(name = "criado em {0}, a primeira vigência é a segunda {1}")
	@CsvSource({
			"2026-10-19, 2026-10-19",
			"2026-10-21, 2026-10-19",
			"2026-10-25, 2026-10-19",
			"2026-10-26, 2026-10-26"
	})
	void criarHabitoSemanalCriaAMetaNaSegundaFeiraDaSemanaDeCriacao(LocalDate hoje, LocalDate segundaEsperada) {
		salvarHabitoComId(10L);

		HabitoResponse resposta = servico(hoje).criar(USUARIO, new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, hoje));

		assertThat(resposta.vezesPorSemana()).isEqualTo(3);
		ArgumentCaptor<HabitoMeta> meta = ArgumentCaptor.forClass(HabitoMeta.class);
		verify(metas).save(meta.capture());
		assertThat(meta.getValue().getHabitoId()).isEqualTo(10L);
		assertThat(meta.getValue().getVezesPorSemana()).isEqualTo(3);
		assertThat(meta.getValue().getVigenteDesde()).isEqualTo(segundaEsperada);
	}

	@Test
	void criarHabitoSemanalSemMetaLancaValidacaoNoCampoVezesPorSemana() {
		assertValidacao(() -> servico().criar(USUARIO, new HabitoRequest("Academia", TipoHabito.SEMANAL, null, HOJE)),
				"vezesPorSemana");
	}

	@Test
	void criarHabitoSemanalSemDataLancaValidacaoNoCampoData() {
		assertValidacao(() -> servico().criar(USUARIO, new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, null)),
				"data");
	}

	@Test
	void criarHabitoSemanalComDataMuitoDistanteLancaValidacaoNoCampoData() {
		assertValidacao(() -> servico().criar(USUARIO,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, HOJE.plusDays(3))), "data");
		assertValidacao(() -> servico().criar(USUARIO,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, HOJE.minusDays(3))), "data");
	}

	@Test
	void criarHabitoSemanalAceitaDataAteDoisDiasDeDiferencaDoServidor() {
		salvarHabitoComId(10L);

		assertThatCode(() -> servico().criar(USUARIO,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, HOJE.plusDays(2)))).doesNotThrowAnyException();
		assertThatCode(() -> servico().criar(USUARIO,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, HOJE.minusDays(2)))).doesNotThrowAnyException();
	}

	@Test
	void criarHabitoDiarioComMetaLancaValidacaoNoCampoVezesPorSemana() {
		assertValidacao(() -> servico().criar(USUARIO, new HabitoRequest("Beber água", TipoHabito.DIARIO, 3, HOJE)),
				"vezesPorSemana");
	}

	@Test
	void criarHabitoDiarioIgnoraAData() {
		salvarHabitoComId(10L);

		assertThatCode(() -> servico().criar(USUARIO,
				new HabitoRequest("Beber água", TipoHabito.DIARIO, null, LocalDate.of(2020, 1, 1))))
				.doesNotThrowAnyException();
	}

	@Test
	void criarHabitoGravaONomeSemEspacosNasPontas() {
		salvarHabitoComId(10L);

		servico().criar(USUARIO, new HabitoRequest("  Beber água  ", TipoHabito.DIARIO, null, null));

		ArgumentCaptor<Habito> salvo = ArgumentCaptor.forClass(Habito.class);
		verify(habitos).save(salvo.capture());
		assertThat(salvo.getValue().getNome()).isEqualTo("Beber água");
	}

	@Test
	void listarConsultaSoOsHabitosDoUsuarioRecebido() {
		when(habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of());

		List<HabitoResponse> lista = servico().listar(USUARIO);

		assertThat(lista).isEmpty();
		verify(habitos).findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO);
	}

	@Test
	void listarSemHabitosNaoConsultaAsMetas() {
		when(habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of());

		servico().listar(USUARIO);

		verifyNoInteractions(metas);
	}

	@Test
	void listarTrazAMetaMaisRecenteDoSemanalEOmiteNoDiario() {
		Habito diario = habito(1L, "Beber água", TipoHabito.DIARIO);
		Habito semanal = habito(2L, "Academia", TipoHabito.SEMANAL);
		when(habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO))
				.thenReturn(List.of(diario, semanal));
		// Vem da vigência mais recente para a mais antiga: a de 26/10 (5 vezes) vale mais que a de 19/10 (3 vezes).
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L, 2L))).thenReturn(List.of(
				meta(2L, 5, LocalDate.of(2026, 10, 26)),
				meta(2L, 3, LocalDate.of(2026, 10, 19))));

		List<HabitoResponse> lista = servico().listar(USUARIO);

		assertThat(lista).containsExactly(
				new HabitoResponse(1L, "Beber água", TipoHabito.DIARIO, null),
				new HabitoResponse(2L, "Academia", TipoHabito.SEMANAL, 5));
	}

	// ---- Home do dia ----

	private static final LocalDate SEGUNDA = LocalDate.of(2026, 10, 19);

	private RegistroHabito registro(Long habitoId, LocalDate data) {
		RegistroHabito registro = new RegistroHabito();
		registro.setHabitoId(habitoId);
		registro.setData(data);
		return registro;
	}

	private void habitosDoUsuario(Habito... lista) {
		when(habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of(lista));
	}

	@Test
	void listarDoDiaMarcaFeitoSoNoDiarioComRegistroNaData() {
		habitosDoUsuario(habito(1L, "Ler", TipoHabito.DIARIO), habito(2L, "Beber água", TipoHabito.DIARIO));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L, 2L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L, 2L), SEGUNDA, SEGUNDA.plusDays(6)))
				.thenReturn(List.of(registro(1L, HOJE), registro(2L, HOJE.minusDays(1))));

		List<HabitoDoDia> lista = servico().listarDoDia(USUARIO, HOJE);

		assertThat(lista).containsExactly(
				new HabitoDoDia(1L, "Ler", TipoHabito.DIARIO, true, null, null),
				new HabitoDoDia(2L, "Beber água", TipoHabito.DIARIO, false, null, null));
	}

	@Test
	void listarDoDiaContaOsRegistrosDaSemanaDoSemanalDeSegundaADomingo() {
		habitosDoUsuario(habito(2L, "Academia", TipoHabito.SEMANAL));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L)))
				.thenReturn(List.of(meta(2L, 3, SEGUNDA)));
		// A consulta pede a semana inteira (19/10 a 25/10): dois dias antes de hoje e um depois contam.
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), SEGUNDA, LocalDate.of(2026, 10, 25)))
				.thenReturn(List.of(registro(2L, SEGUNDA), registro(2L, HOJE), registro(2L, HOJE.plusDays(1))));

		List<HabitoDoDia> lista = servico().listarDoDia(USUARIO, HOJE);

		assertThat(lista).containsExactly(new HabitoDoDia(2L, "Academia", TipoHabito.SEMANAL, true, 3, 3));
	}

	@Test
	void listarDoDiaSemanalSemRegistrosTemZeroFeitos() {
		habitosDoUsuario(habito(2L, "Academia", TipoHabito.SEMANAL));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L)))
				.thenReturn(List.of(meta(2L, 3, SEGUNDA)));
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), SEGUNDA, SEGUNDA.plusDays(6)))
				.thenReturn(List.of());

		List<HabitoDoDia> lista = servico().listarDoDia(USUARIO, HOJE);

		assertThat(lista).containsExactly(new HabitoDoDia(2L, "Academia", TipoHabito.SEMANAL, false, 0, 3));
	}

	@Test
	void listarDoDiaUsaAMetaQueValiaNaSemanaEIgnoraAVigenciaFutura() {
		habitosDoUsuario(habito(2L, "Academia", TipoHabito.SEMANAL));
		// A meta nova (5) só vale da segunda 26/10; a semana de hoje (19/10) mantém a antiga (3).
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L))).thenReturn(List.of(
				meta(2L, 5, LocalDate.of(2026, 10, 26)),
				meta(2L, 3, SEGUNDA),
				meta(2L, 2, LocalDate.of(2026, 10, 5))));
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), SEGUNDA, SEGUNDA.plusDays(6)))
				.thenReturn(List.of());

		List<HabitoDoDia> lista = servico().listarDoDia(USUARIO, HOJE);

		assertThat(lista.get(0).meta()).isEqualTo(3);
	}

	@Test
	void listarDoDiaNaSemanaSeguinteUsaAMetaNova() {
		LocalDate segundaSeguinte = LocalDate.of(2026, 10, 26);
		habitosDoUsuario(habito(2L, "Academia", TipoHabito.SEMANAL));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L))).thenReturn(List.of(
				meta(2L, 5, segundaSeguinte),
				meta(2L, 3, SEGUNDA)));
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), segundaSeguinte, segundaSeguinte.plusDays(6)))
				.thenReturn(List.of());

		// Hoje é domingo 25/10, e o front já olha a segunda 26/10 (dentro da tolerância de 2 dias).
		List<HabitoDoDia> lista = servico(LocalDate.of(2026, 10, 25)).listarDoDia(USUARIO, segundaSeguinte);

		assertThat(lista).containsExactly(new HabitoDoDia(2L, "Academia", TipoHabito.SEMANAL, false, 0, 5));
	}

	@Test
	void listarDoDiaOmiteSemanalSemMetaVigenteNaSemana() {
		habitosDoUsuario(habito(2L, "Academia", TipoHabito.SEMANAL));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L)))
				.thenReturn(List.of(meta(2L, 3, LocalDate.of(2026, 10, 26))));
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), SEGUNDA, SEGUNDA.plusDays(6)))
				.thenReturn(List.of());

		assertThat(servico().listarDoDia(USUARIO, HOJE)).isEmpty();
	}

	@Test
	void listarDoDiaConsultaSoOsHabitosDoUsuarioESemHabitosNaoConsultaMetasNemRegistros() {
		when(habitos.findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of());

		assertThat(servico().listarDoDia(USUARIO, HOJE)).isEmpty();

		verify(habitos).findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(USUARIO);
		verifyNoInteractions(metas, registros);
	}

	@Test
	void listarDoDiaComDataForaDaToleranciaLancaValidacaoNoCampoDataSemConsultar() {
		assertThatThrownBy(() -> servico().listarDoDia(USUARIO, HOJE.minusDays(3)))
				.isInstanceOfSatisfying(ValidacaoException.class, e -> assertThat(e.getCampo()).isEqualTo("data"));

		verifyNoInteractions(habitos, metas, registros);
	}

	// ---- Insights ----

	private static final LocalDate DE_INSIGHTS = LocalDate.of(2026, 10, 1);
	private static final LocalDate ATE_INSIGHTS = LocalDate.of(2026, 10, 31);

	private Habito habitoCriadoEm(Long id, TipoHabito tipo, String criadoEmUtc, String excluidoEmUtc) {
		Habito habito = habito(id, "H" + id, tipo);
		habito.setCriadoEm(Instant.parse(criadoEmUtc));
		if (excluidoEmUtc != null) {
			habito.setExcluidoEm(Instant.parse(excluidoEmUtc));
		}
		return habito;
	}

	private void todosOsHabitosDoUsuario(Habito... lista) {
		when(habitos.findByUsuarioIdOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of(lista));
	}

	@Test
	void historicoConverteCriadoEmEExcluidoEmParaODiaNoFusoDoUsuario() {
		// 02/10 às 01:30 UTC ainda é 01/10 às 22:30 em São Paulo; 20/10 às 02:00 UTC é 19/10 às 23:00.
		todosOsHabitosDoUsuario(habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-10-02T01:30:00Z", "2026-10-20T02:00:00Z"));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of());

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		assertThat(historico).hasSize(1);
		assertThat(historico.get(0).criadoDia()).isEqualTo(LocalDate.of(2026, 10, 1));
		assertThat(historico.get(0).excluidoDia()).isEqualTo(LocalDate.of(2026, 10, 19));
	}

	@Test
	void historicoUsaOFusoRecebidoParaConverterOsDias() {
		// 02/10 às 01:30 UTC: em São Paulo ainda é 01/10 (22:30), mas em Tóquio já é 02/10 (10:30).
		todosOsHabitosDoUsuario(habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-10-02T01:30:00Z", null));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of());

		List<HabitoHistorico> emToquio = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS,
				ZoneId.of("Asia/Tokyo"));

		assertThat(emToquio.get(0).criadoDia()).isEqualTo(LocalDate.of(2026, 10, 2));
	}

	@Test
	void historicoTrazHabitoNaoExcluidoComExcluidoDiaNulo() {
		todosOsHabitosDoUsuario(habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-09-10T15:00:00Z", null));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of());

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		assertThat(historico.get(0).excluidoDia()).isNull();
		assertThat(historico.get(0).nome()).isEqualTo("H1");
		assertThat(historico.get(0).tipo()).isEqualTo(TipoHabito.DIARIO);
	}

	@Test
	void historicoOmiteHabitoExcluidoAntesDoPeriodoOuNoPrimeiroDia() {
		// Excluído em 30/09 e excluído em 01/10 (o dia da exclusão não conta): nenhum existiu em 01/10..31/10.
		todosOsHabitosDoUsuario(
				habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-08-01T15:00:00Z", "2026-09-30T15:00:00Z"),
				habitoCriadoEm(2L, TipoHabito.DIARIO, "2026-08-01T15:00:00Z", "2026-10-01T15:00:00Z"));

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		assertThat(historico).isEmpty();
		verifyNoInteractions(metas, registros);
	}

	@Test
	void historicoOmiteHabitoCriadoDepoisDoPeriodoEMantemOCriadoNoUltimoDia() {
		todosOsHabitosDoUsuario(
				habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-11-01T15:00:00Z", null),
				habitoCriadoEm(2L, TipoHabito.DIARIO, "2026-10-31T15:00:00Z", null));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(2L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(2L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of());

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		assertThat(historico).extracting(HabitoHistorico::id).containsExactly(2L);
	}

	@Test
	void historicoMantemHabitoExcluidoNoMeioDoPeriodo() {
		todosOsHabitosDoUsuario(habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-09-01T15:00:00Z", "2026-10-15T15:00:00Z"));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L))).thenReturn(List.of());
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of());

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		assertThat(historico).extracting(HabitoHistorico::excluidoDia).containsExactly(LocalDate.of(2026, 10, 15));
	}

	@Test
	void historicoAgrupaMetasEDiasFeitosPorHabitoEConsultaSoOUsuarioRecebido() {
		todosOsHabitosDoUsuario(
				habitoCriadoEm(1L, TipoHabito.DIARIO, "2026-09-01T15:00:00Z", null),
				habitoCriadoEm(2L, TipoHabito.SEMANAL, "2026-09-01T15:00:00Z", null));
		when(metas.findByHabitoIdInOrderByVigenteDesdeDesc(List.of(1L, 2L))).thenReturn(List.of(
				meta(2L, 5, LocalDate.of(2026, 10, 12)),
				meta(2L, 3, LocalDate.of(2026, 8, 31))));
		when(registros.findByHabitoIdInAndDataBetween(List.of(1L, 2L), DE_INSIGHTS, ATE_INSIGHTS)).thenReturn(List.of(
				registro(1L, LocalDate.of(2026, 10, 2)),
				registro(1L, LocalDate.of(2026, 10, 3)),
				registro(2L, LocalDate.of(2026, 10, 2))));

		List<HabitoHistorico> historico = servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO);

		HabitoHistorico diario = historico.get(0);
		HabitoHistorico semanal = historico.get(1);
		assertThat(diario.metas()).isEmpty();
		assertThat(diario.diasFeitos()).containsExactlyInAnyOrder(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3));
		assertThat(semanal.metas()).containsExactly(
				new HabitoHistorico.MetaVigente(LocalDate.of(2026, 10, 12), 5),
				new HabitoHistorico.MetaVigente(LocalDate.of(2026, 8, 31), 3));
		assertThat(semanal.diasFeitos()).containsExactly(LocalDate.of(2026, 10, 2));
		verify(habitos).findByUsuarioIdOrderByCriadoEmAscIdAsc(USUARIO);
	}

	@Test
	void historicoSemHabitosDevolveListaVaziaSemConsultarMetasNemRegistros() {
		when(habitos.findByUsuarioIdOrderByCriadoEmAscIdAsc(USUARIO)).thenReturn(List.of());

		assertThat(servico().historicoParaInsights(USUARIO, DE_INSIGHTS, ATE_INSIGHTS, SAO_PAULO)).isEmpty();

		verifyNoInteractions(metas, registros);
	}

	// ---- edição ----

	private void acharHabito(Habito habito) {
		when(habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(habito.getId(), USUARIO)).thenReturn(Optional.of(habito));
	}

	@Test
	void atualizarTrocaONomeEApagaOsEspacosDasPontas() {
		Habito diario = habito(10L, "Beber agua", TipoHabito.DIARIO);
		acharHabito(diario);

		HabitoResponse resposta = servico().atualizar(USUARIO, 10L,
				new HabitoRequest("  Beber água  ", TipoHabito.DIARIO, null, HOJE));

		assertThat(resposta.nome()).isEqualTo("Beber água");
		assertThat(diario.getNome()).isEqualTo("Beber água");
		verifyNoInteractions(metas);
	}

	@Test
	void atualizarComTipoDiferenteDoGravadoLancaValidacaoNoCampoTipoENaoAlteraNada() {
		Habito diario = habito(10L, "Beber água", TipoHabito.DIARIO);
		acharHabito(diario);

		assertValidacao(() -> servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Outro nome", TipoHabito.SEMANAL, 3, HOJE)), "tipo");

		assertThat(diario.getNome()).isEqualTo("Beber água");
		assertThat(diario.getTipo()).isEqualTo(TipoHabito.DIARIO);
		verifyNoInteractions(metas);
	}

	@Test
	void atualizarHabitoQueOFiltroNaoAchaLancaNaoEncontradoSemGravar() {
		// De outro usuário, excluído ou inexistente: o Repository devolve vazio nos três casos.
		when(habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(10L, USUARIO)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Qualquer", TipoHabito.DIARIO, null, HOJE)))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(habitos).findByIdAndUsuarioIdAndExcluidoEmIsNull(10L, USUARIO);
		verify(habitos, never()).save(any());
		verifyNoInteractions(metas);
	}

	@Test
	void atualizarHabitoDiarioComMetaLancaValidacaoENaoMudaONome() {
		Habito diario = habito(10L, "Beber água", TipoHabito.DIARIO);
		acharHabito(diario);

		assertValidacao(() -> servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Novo nome", TipoHabito.DIARIO, 3, HOJE)), "vezesPorSemana");

		assertThat(diario.getNome()).isEqualTo("Beber água");
	}

	@Test
	void atualizarHabitoSemanalSemMetaOuSemDataLancaValidacao() {
		acharHabito(habito(10L, "Academia", TipoHabito.SEMANAL));

		assertValidacao(() -> servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, null, HOJE)), "vezesPorSemana");
		assertValidacao(() -> servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 3, null)), "data");
	}

	@ParameterizedTest(name = "hoje {0}: a meta nova começa na segunda {1}")
	@CsvSource({
			"2026-10-19, 2026-10-26",
			"2026-10-21, 2026-10-26",
			"2026-10-25, 2026-10-26"
	})
	void atualizarMetaCriaAVigenciaNaProximaSegundaFeira(LocalDate hoje, LocalDate proximaSegunda) {
		acharHabito(habito(10L, "Academia", TipoHabito.SEMANAL));
		when(metas.findByHabitoIdAndVigenteDesde(10L, proximaSegunda)).thenReturn(Optional.empty());
		when(metas.findFirstByHabitoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(10L, proximaSegunda))
				.thenReturn(Optional.of(meta(10L, 3, LocalDate.of(2026, 10, 19))));

		HabitoResponse resposta = servico(hoje).atualizar(USUARIO, 10L,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 5, hoje));

		assertThat(resposta.vezesPorSemana()).isEqualTo(5);
		ArgumentCaptor<HabitoMeta> nova = ArgumentCaptor.forClass(HabitoMeta.class);
		verify(metas).save(nova.capture());
		assertThat(nova.getValue().getHabitoId()).isEqualTo(10L);
		assertThat(nova.getValue().getVezesPorSemana()).isEqualTo(5);
		assertThat(nova.getValue().getVigenteDesde()).isEqualTo(proximaSegunda);
	}

	@Test
	void atualizarMetaSubstituiAVigenciaFuturaQueJaExisteEmVezDeCriarOutra() {
		acharHabito(habito(10L, "Academia", TipoHabito.SEMANAL));
		HabitoMeta futura = meta(10L, 5, LocalDate.of(2026, 10, 26));
		when(metas.findByHabitoIdAndVigenteDesde(10L, LocalDate.of(2026, 10, 26))).thenReturn(Optional.of(futura));

		HabitoResponse resposta = servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Academia", TipoHabito.SEMANAL, 4, HOJE));

		assertThat(resposta.vezesPorSemana()).isEqualTo(4);
		ArgumentCaptor<HabitoMeta> salva = ArgumentCaptor.forClass(HabitoMeta.class);
		verify(metas).save(salva.capture());
		assertThat(salva.getValue()).isSameAs(futura);
		assertThat(futura.getVezesPorSemana()).isEqualTo(4);
		assertThat(futura.getVigenteDesde()).isEqualTo(LocalDate.of(2026, 10, 26));
	}

	@Test
	void atualizarComMetaIgualAQueJaVaiValerNaoCriaVigenciaRepetida() {
		acharHabito(habito(10L, "Academia", TipoHabito.SEMANAL));
		LocalDate proximaSegunda = LocalDate.of(2026, 10, 26);
		when(metas.findByHabitoIdAndVigenteDesde(10L, proximaSegunda)).thenReturn(Optional.empty());
		when(metas.findFirstByHabitoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(10L, proximaSegunda))
				.thenReturn(Optional.of(meta(10L, 3, LocalDate.of(2026, 10, 19))));

		HabitoResponse resposta = servico().atualizar(USUARIO, 10L,
				new HabitoRequest("Academia (novo nome)", TipoHabito.SEMANAL, 3, HOJE));

		assertThat(resposta.vezesPorSemana()).isEqualTo(3);
		assertThat(resposta.nome()).isEqualTo("Academia (novo nome)");
		verify(metas, never()).save(any());
	}

	// ---- exclusão ----

	@Test
	void excluirPreencheExcluidoEmComOInstanteAtualENaoApagaNada() {
		Habito diario = habito(10L, "Beber água", TipoHabito.DIARIO);
		acharHabito(diario);

		servico().excluir(USUARIO, 10L);

		assertThat(diario.getExcluidoEm()).isEqualTo(HOJE.atTime(12, 0).toInstant(ZoneOffset.UTC));
		verify(habitos, never()).delete(any());
		verify(habitos, never()).deleteById(any());
		verifyNoInteractions(metas);
	}

	@Test
	void excluirHabitoQueOFiltroNaoAchaLancaNaoEncontradoESoConsultaPeloUsuarioRecebido() {
		when(habitos.findByIdAndUsuarioIdAndExcluidoEmIsNull(10L, USUARIO)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico().excluir(USUARIO, 10L))
				.isInstanceOf(RecursoNaoEncontradoException.class);

		verify(habitos).findByIdAndUsuarioIdAndExcluidoEmIsNull(10L, USUARIO);
		verify(habitos, never()).delete(any());
	}

	@Test
	void apagarTodosDoUsuarioApagaRegistrosMetasEHabitosNessaOrdem() {
		servico().apagarTodosDoUsuario(USUARIO);

		InOrder ordem = inOrder(registros, metas, habitos);
		ordem.verify(registros).apagarDoUsuario(USUARIO);
		ordem.verify(metas).apagarDoUsuario(USUARIO);
		ordem.verify(habitos).apagarDoUsuario(USUARIO);
	}

}
