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

@ExtendWith(MockitoExtension.class)
class HabitoServiceTest {

	private static final Long USUARIO = 7L;
	// 21/10/2026 é quarta-feira; a segunda da semana é 19/10/2026.
	private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

	@Mock
	private HabitoRepository habitos;

	@Mock
	private HabitoMetaRepository metas;

	private HabitoService servico(LocalDate hoje) {
		Clock relogio = Clock.fixed(hoje.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
		return new HabitoService(habitos, metas, relogio);
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

}
