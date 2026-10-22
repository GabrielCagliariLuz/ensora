package com.ensora.insights;

import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.HabitoHistorico.MetaVigente;
import com.ensora.habito.TipoHabito;
import com.ensora.insights.ConsistenciaCalculadora.Celula;
import com.ensora.insights.ConsistenciaCalculadora.Estado;
import com.ensora.insights.ConsistenciaCalculadora.HabitoConsistencia;
import com.ensora.insights.ConsistenciaCalculadora.SemanaDoHabito;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistenciaCalculadoraTest {

	// Setembro de 2026 começa numa terça; as segundas são 31/08, 07, 14, 21 e 28/09.
	private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);
	private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
	private static final LocalDate HOJE_DEPOIS = LocalDate.of(2026, 10, 21); // setembro já terminou
	private static final LocalDate ANTES = LocalDate.of(2026, 8, 1);

	private static LocalDate set(int dia) {
		return LocalDate.of(2026, 9, dia);
	}

	private static LocalDate out(int dia) {
		return LocalDate.of(2026, 10, dia);
	}

	private HabitoHistorico diario(LocalDate criadoDia, LocalDate excluidoDia, LocalDate... feitos) {
		return new HabitoHistorico(1L, "Ler", TipoHabito.DIARIO, criadoDia, excluidoDia, List.of(),
				new HashSet<>(List.of(feitos)));
	}

	private HabitoHistorico semanal(LocalDate criadoDia, LocalDate excluidoDia, List<MetaVigente> metas,
			LocalDate... feitos) {
		return new HabitoHistorico(2L, "Academia", TipoHabito.SEMANAL, criadoDia, excluidoDia, metas,
				new HashSet<>(List.of(feitos)));
	}

	private List<MetaVigente> meta(LocalDate vigenteDesde, int vezes) {
		return List.of(new MetaVigente(vigenteDesde, vezes));
	}

	private HabitoConsistencia unico(List<HabitoHistorico> historico, YearMonth mes, LocalDate hoje) {
		List<HabitoConsistencia> lista = ConsistenciaCalculadora.calcular(historico, mes, hoje);
		assertThat(lista).hasSize(1);
		return lista.get(0);
	}

	private Set<LocalDate> datasCom(HabitoConsistencia habito, Estado estado) {
		return habito.dias().stream().filter(c -> c.estado() == estado).map(Celula::data).collect(Collectors.toSet());
	}

	private Set<LocalDate> faixa(LocalDate de, LocalDate ate) {
		return de.datesUntil(ate.plusDays(1)).collect(Collectors.toSet());
	}

	private Set<LocalDate> juntar(Set<LocalDate> a, Set<LocalDate> b) {
		Set<LocalDate> uniao = new HashSet<>(a);
		uniao.addAll(b);
		return uniao;
	}

	// ---- diário: consistência ----

	@Test
	void diarioNumMesPassadoInteiroUsaTodosOsDiasDoMes() {
		LocalDate[] feitos = faixa(set(1), set(15)).toArray(new LocalDate[0]);

		HabitoConsistencia h = unico(List.of(diario(ANTES, null, feitos)), SETEMBRO, HOJE_DEPOIS);

		assertThat(h.consistencia()).isEqualTo(50.0);
		assertThat(h.dias()).hasSize(30);
		assertThat(datasCom(h, Estado.CONCLUIDO)).isEqualTo(faixa(set(1), set(15)));
		assertThat(datasCom(h, Estado.NAO_REALIZADO)).isEqualTo(faixa(set(16), set(30)));
	}

	@Test
	void diarioCriadoNoMeioDoMesNaoEPenalizadoPelosDiasAnteriores() {
		LocalDate[] feitos = faixa(set(10), set(16)).toArray(new LocalDate[0]);

		HabitoConsistencia h = unico(List.of(diario(set(10), null, feitos)), SETEMBRO, HOJE_DEPOIS);

		// 7 feitos em 21 dias considerados (10 a 30/09); nada de 1 a 9/09.
		assertThat(h.consistencia()).isEqualTo(33.3);
		assertThat(h.dias().get(0).data()).isEqualTo(set(10));
		assertThat(h.dias()).hasSize(21);
	}

	@Test
	void diarioExcluidoNoMeioDoMesNaoContaODiaDaExclusaoNemOsSeguintes() {
		LocalDate[] feitos = faixa(set(1), set(10)).toArray(new LocalDate[0]);

		HabitoConsistencia h = unico(List.of(diario(ANTES, set(20), feitos)), SETEMBRO, HOJE_DEPOIS);

		// Dias 1 a 19: 10 feitos em 19 = 52,6.
		assertThat(h.consistencia()).isEqualTo(52.6);
		assertThat(h.dias().get(h.dias().size() - 1).data()).isEqualTo(set(19));
		assertThat(h.excluido()).isTrue();
	}

	@Test
	void diarioNoMesEmAndamentoContaAteHojeInclusive() {
		LocalDate hoje = out(10);
		LocalDate[] feitos = faixa(out(1), out(5)).toArray(new LocalDate[0]);

		HabitoConsistencia h = unico(List.of(diario(ANTES, null, feitos)), OUTUBRO, hoje);

		// 5 feitos em 10 dias (1 a 10/10, hoje incluído).
		assertThat(h.consistencia()).isEqualTo(50.0);
		assertThat(h.dias()).hasSize(31);
	}

	@Test
	void diarioMesFuturoTemCelulasDeDescansoESemConsistencia() {
		HabitoConsistencia h = unico(List.of(diario(ANTES, null)), YearMonth.of(2026, 11), HOJE_DEPOIS);

		assertThat(h.consistencia()).isNull();
		assertThat(h.dias()).hasSize(30);
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).hasSize(30);
	}

	@Test
	void diarioArredondaParaUmaCasaComMeioParaCima() {
		// Criado em 15/09: janela de 16 dias (15 a 30) com 1 feito = 6,25 -> 6,3.
		HabitoConsistencia h = unico(List.of(diario(set(15), null, set(20))), SETEMBRO, HOJE_DEPOIS);

		assertThat(h.consistencia()).isEqualTo(6.3);
	}

	@Test
	void habitoNaoExcluidoTemExcluidoFalso() {
		assertThat(unico(List.of(diario(ANTES, null)), SETEMBRO, HOJE_DEPOIS).excluido()).isFalse();
	}

	// ---- diário: estados das células ----

	@Test
	void diarioHojeSemRegistroEstaEmAbertoOsPassadosSemRegistroNaoForamRealizadosEOsFuturosDescansam() {
		LocalDate hoje = out(10);

		HabitoConsistencia h = unico(List.of(diario(ANTES, null, out(1))), OUTUBRO, hoje);

		assertThat(datasCom(h, Estado.CONCLUIDO)).containsExactly(out(1));
		assertThat(datasCom(h, Estado.NAO_REALIZADO)).isEqualTo(faixa(out(2), out(9)));
		assertThat(datasCom(h, Estado.EM_ABERTO)).containsExactly(out(10));
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).isEqualTo(faixa(out(11), out(31)));
	}

	@Test
	void diarioHojeJaFeitoEstaConcluidoENaoEmAberto() {
		LocalDate hoje = out(10);

		HabitoConsistencia h = unico(List.of(diario(ANTES, null, hoje)), OUTUBRO, hoje);

		assertThat(datasCom(h, Estado.CONCLUIDO)).containsExactly(hoje);
		assertThat(datasCom(h, Estado.EM_ABERTO)).isEmpty();
	}

	@Test
	void diarioMarcadoEmDiaFuturoFicaConcluidoMasNaoEntraNaConsistencia() {
		LocalDate hoje = out(10);

		HabitoConsistencia h = unico(List.of(diario(ANTES, null, out(12))), OUTUBRO, hoje);

		assertThat(datasCom(h, Estado.CONCLUIDO)).containsExactly(out(12));
		assertThat(h.consistencia()).isEqualTo(0.0);
	}

	// ---- quem aparece ----

	@Test
	void habitoCriadoDepoisDoMesOuExcluidoAntesDeleNaoAparece() {
		HabitoHistorico criadoDepois = diario(out(5), null);
		HabitoHistorico excluidoAntes = diario(ANTES, set(1));

		assertThat(ConsistenciaCalculadora.calcular(List.of(criadoDepois, excluidoAntes), SETEMBRO, HOJE_DEPOIS))
				.isEmpty();
	}

	@Test
	void habitoExcluidoApareceNosMesesEmQueExistiaENaoNosSeguintes() {
		HabitoHistorico excluido = diario(ANTES, set(20), set(1));

		assertThat(ConsistenciaCalculadora.calcular(List.of(excluido), SETEMBRO, HOJE_DEPOIS)).hasSize(1);
		assertThat(ConsistenciaCalculadora.calcular(List.of(excluido), OUTUBRO, HOJE_DEPOIS)).isEmpty();
	}

	@Test
	void mantemAOrdemRecebida() {
		HabitoHistorico primeiro = new HabitoHistorico(5L, "A", TipoHabito.DIARIO, ANTES, null, List.of(), Set.of());
		HabitoHistorico segundo = new HabitoHistorico(3L, "B", TipoHabito.DIARIO, ANTES, null, List.of(), Set.of());

		assertThat(ConsistenciaCalculadora.calcular(List.of(primeiro, segundo), SETEMBRO, HOJE_DEPOIS))
				.extracting(HabitoConsistencia::id).containsExactly(5L, 3L);
	}

	// ---- semanal ----

	// Roteiro de verificação: meta 2 desde 31/08, criado em 01/09; feitos em 08, 09, 15, 22, 25 e 30/09.
	private HabitoHistorico semanalDoRoteiro() {
		return semanal(set(1), null, meta(LocalDate.of(2026, 8, 31), 2), set(8), set(9), set(15), set(22), set(25), set(30));
	}

	@Test
	void semanalConsistenciaContaSemanasComMetaBatidaSobreSemanasConsideradas() {
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), SETEMBRO, HOJE_DEPOIS);

		// Semanas de 07, 14, 21 e 28/09 (a de 31/08 pertence a agosto): batidas as de 07 e 21 = 2/4.
		assertThat(h.consistencia()).isEqualTo(50.0);
	}

	@Test
	void semanalResumoDasSemanasTrazFeitosMetaEMarcaQuaisContamNoMes() {
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), SETEMBRO, HOJE_DEPOIS);

		assertThat(h.semanas()).containsExactly(
				new SemanaDoHabito(LocalDate.of(2026, 8, 31), set(6), 2, 0, false, true, false),
				new SemanaDoHabito(set(7), set(13), 2, 2, true, true, true),
				new SemanaDoHabito(set(14), set(20), 2, 1, false, true, true),
				new SemanaDoHabito(set(21), set(27), 2, 2, true, true, true),
				new SemanaDoHabito(set(28), LocalDate.of(2026, 10, 4), 2, 1, false, true, true));
	}

	@Test
	void semanalSemanaEncerradaSemMetaPintaDeVermelhoTodosOsDiasSemRegistro() {
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), SETEMBRO, HOJE_DEPOIS);

		Set<LocalDate> vermelhos = juntar(juntar(faixa(set(1), set(6)), Set.of(set(14))), Set.of(set(16), set(17), set(18),
				set(19), set(20), set(28), set(29)));
		assertThat(datasCom(h, Estado.NAO_REALIZADO)).isEqualTo(vermelhos);
		assertThat(datasCom(h, Estado.CONCLUIDO))
				.containsExactlyInAnyOrder(set(8), set(9), set(15), set(22), set(25), set(30));
	}

	@Test
	void semanalSemanaComMetaBatidaDeixaOsOutrosDiasNeutros() {
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), SETEMBRO, HOJE_DEPOIS);

		// Semanas de 07 e 21/09 bateram a meta: os dias sem registro delas descansam.
		Set<LocalDate> descanso = juntar(Set.of(set(7), set(10), set(11), set(12), set(13)),
				Set.of(set(21), set(23), set(24), set(26), set(27)));
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).isEqualTo(descanso);
	}

	@Test
	void semanalSemanaEmAndamentoNaoFicaVermelhaNemEntraNaConsistenciaSeNaoBateuAMeta() {
		LocalDate hoje = out(21); // quarta; semana de 19 a 25/10 em andamento
		HabitoHistorico s = semanal(out(19), null, meta(out(19), 2), out(19));

		HabitoConsistencia h = unico(List.of(s), OUTUBRO, hoje);

		assertThat(datasCom(h, Estado.NAO_REALIZADO)).isEmpty();
		assertThat(datasCom(h, Estado.CONCLUIDO)).containsExactly(out(19));
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).contains(out(20), out(22), out(25));
		assertThat(datasCom(h, Estado.EM_ABERTO)).containsExactly(out(21));
		assertThat(h.semanas().get(0).contaNaConsistencia()).isFalse();
		assertThat(h.consistencia()).isNull();
	}

	@Test
	void semanalSemanaEmAndamentoComMetaJaBatidaContaEHojeSemRegistroDescansa() {
		LocalDate hoje = out(21);
		HabitoHistorico s = semanal(out(19), null, meta(out(19), 2), out(19), out(20));

		HabitoConsistencia h = unico(List.of(s), OUTUBRO, hoje);

		assertThat(datasCom(h, Estado.EM_ABERTO)).isEmpty();
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).contains(hoje);
		assertThat(h.semanas().get(0).batida()).isTrue();
		assertThat(h.semanas().get(0).contaNaConsistencia()).isTrue();
		assertThat(h.consistencia()).isEqualTo(100.0);
	}

	@Test
	void semanalSemanaAtravessandoOMesPertenceAoMesDaSegundaMasSuasCelulasAparecemNosDois() {
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), OUTUBRO, HOJE_DEPOIS);

		// Em outubro, a semana de 28/09 a 04/10 (segunda em setembro) dá células de 1 a 4/10 e não conta.
		SemanaDoHabito atravessando = h.semanas().get(0);
		assertThat(atravessando.de()).isEqualTo(set(28));
		assertThat(atravessando.ate()).isEqualTo(out(4));
		assertThat(atravessando.contaNaConsistencia()).isFalse();
		assertThat(h.dias().get(0).data()).isEqualTo(out(1));
	}

	@Test
	void semanalSemanaDoFimDoMesQueAindaNaoTerminouNaoPenalizaOMes() {
		// Hoje 21/10: a semana de 26/10 a 01/11 tem segunda em outubro, ainda não encerrou e não bateu a meta.
		HabitoConsistencia h = unico(List.of(semanalDoRoteiro()), OUTUBRO, HOJE_DEPOIS);

		SemanaDoHabito ultima = h.semanas().get(h.semanas().size() - 1);
		assertThat(ultima.de()).isEqualTo(out(26));
		assertThat(ultima.encerrada()).isFalse();
		assertThat(ultima.contaNaConsistencia()).isFalse();
		assertThat(datasCom(h, Estado.DESCANSO_FUTURO)).containsAll(faixa(out(26), out(31)));
	}

	@Test
	void semanalComMetaQueMudaUsaAMetaVigenteEmCadaSemana() {
		List<MetaVigente> metas = List.of(new MetaVigente(set(14), 3), new MetaVigente(LocalDate.of(2026, 8, 31), 2));
		// Semana de 07/09: 2 feitos, meta 2 (batida). Semana de 14/09: 2 feitos, meta 3 (não batida).
		HabitoHistorico s = semanal(ANTES, null, metas, set(8), set(9), set(15), set(16));

		HabitoConsistencia h = unico(List.of(s), SETEMBRO, HOJE_DEPOIS);

		SemanaDoHabito semana07 = h.semanas().stream().filter(w -> w.de().equals(set(7))).findFirst().orElseThrow();
		SemanaDoHabito semana14 = h.semanas().stream().filter(w -> w.de().equals(set(14))).findFirst().orElseThrow();
		assertThat(semana07.meta()).isEqualTo(2);
		assertThat(semana07.batida()).isTrue();
		assertThat(semana14.meta()).isEqualTo(3);
		assertThat(semana14.batida()).isFalse();
	}

	@Test
	void semanalSemMetaVigenteNaSemanaNaoGeraCelulaNemResumo() {
		// A primeira meta vale desde 14/09: antes disso o semanal não existia.
		HabitoHistorico s = semanal(set(1), null, meta(set(14), 2));

		HabitoConsistencia h = unico(List.of(s), SETEMBRO, HOJE_DEPOIS);

		assertThat(h.dias().get(0).data()).isEqualTo(set(14));
		assertThat(h.semanas().get(0).de()).isEqualTo(set(14));
	}

	@Test
	void semanalRegistroAnteriorAAcriacaoNaoContaParaAMetaDaSemana() {
		// Criado na quarta 09/09; o registro de 07/09 (antes de existir) é ignorado: 1 feito < meta 2.
		HabitoHistorico s = semanal(set(9), null, meta(LocalDate.of(2026, 8, 31), 2), set(7), set(9));

		HabitoConsistencia h = unico(List.of(s), SETEMBRO, HOJE_DEPOIS);

		SemanaDoHabito semana07 = h.semanas().get(0);
		assertThat(semana07.de()).isEqualTo(set(7));
		assertThat(semana07.feitos()).isEqualTo(1);
		assertThat(semana07.batida()).isFalse();
		assertThat(h.dias().get(0).data()).isEqualTo(set(9)); // sem células antes da criação
	}

	@Test
	void semanalExcluidoNoMeioDaSemanaTemCelulasSoAteODiaAnteriorEAsSeguintesNaoAparecem() {
		// Excluído na quinta 17/09: a semana de 14/09 tem células de 14 a 16/09; a de 21/09 não existe.
		HabitoHistorico s = semanal(ANTES, set(17), meta(LocalDate.of(2026, 8, 31), 2), set(14));

		HabitoConsistencia h = unico(List.of(s), SETEMBRO, HOJE_DEPOIS);

		assertThat(h.dias().get(h.dias().size() - 1).data()).isEqualTo(set(16));
		assertThat(h.semanas()).extracting(SemanaDoHabito::de).doesNotContain(set(21), set(28));
		assertThat(h.excluido()).isTrue();
	}

}
