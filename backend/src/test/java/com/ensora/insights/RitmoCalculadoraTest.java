package com.ensora.insights;

import com.ensora.habito.HabitoHistorico;
import com.ensora.habito.HabitoHistorico.MetaVigente;
import com.ensora.habito.TipoHabito;
import com.ensora.insights.RitmoCalculadora.BlocoRitmo;
import com.ensora.insights.RitmoCalculadora.DiaRitmo;
import com.ensora.insights.RitmoCalculadora.Resultado;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RitmoCalculadoraTest {

	// 01/10/2026 é quinta-feira; a segunda dessa semana é 28/09/2026.
	private static final LocalDate D1 = LocalDate.of(2026, 10, 1);
	private static final LocalDate D2 = LocalDate.of(2026, 10, 2);
	private static final LocalDate D3 = LocalDate.of(2026, 10, 3);
	private static final LocalDate ANTES = LocalDate.of(2026, 9, 1);
	private static final LocalDate SEGUNDA_28_09 = LocalDate.of(2026, 9, 28);

	private HabitoHistorico diario(long id, LocalDate criadoDia, LocalDate excluidoDia, LocalDate... feitos) {
		return new HabitoHistorico(id, "D" + id, TipoHabito.DIARIO, criadoDia, excluidoDia, List.of(),
				new HashSet<>(List.of(feitos)));
	}

	private HabitoHistorico semanal(long id, LocalDate criadoDia, List<MetaVigente> metas, LocalDate... feitos) {
		return new HabitoHistorico(id, "S" + id, TipoHabito.SEMANAL, criadoDia, null, metas,
				new HashSet<>(List.of(feitos)));
	}

	private List<MetaVigente> meta(LocalDate vigenteDesde, int vezes) {
		return List.of(new MetaVigente(vigenteDesde, vezes));
	}

	private List<Double> ritmos(Resultado resultado) {
		List<Double> lista = new ArrayList<>();
		for (DiaRitmo dia : resultado.dias()) {
			lista.add(dia.ritmo());
		}
		return lista;
	}

	// ---- ritmo do dia ----

	@Test
	void soDiariosDiaFeitoDa100EDiaNaoFeitoDaZero() {
		HabitoHistorico leitura = diario(1, ANTES, null, D1, D2);

		Resultado resultado = RitmoCalculadora.calcular(List.of(leitura), D1, D3);

		assertThat(ritmos(resultado)).containsExactly(100.0, 100.0, 0.0);
		assertThat(resultado.ritmoMedio()).isEqualTo(66.7);
	}

	@Test
	void doisDiariosComUmFeitoDa50() {
		Resultado resultado = RitmoCalculadora.calcular(
				List.of(diario(1, ANTES, null, D1), diario(2, ANTES, null)), D1, D1);

		assertThat(ritmos(resultado)).containsExactly(50.0);
	}

	@Test
	void semanalComMeta7ContaComoUmPorDiaNoDenominador() {
		// diário + semanal meta 7: denominador 1 + 7/7 = 2.
		HabitoHistorico d = diario(1, ANTES, null, D1, D2);
		HabitoHistorico s = semanal(2, ANTES, meta(SEGUNDA_28_09, 7), D1);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d, s), D1, D3);

		assertThat(ritmos(resultado)).containsExactly(100.0, 50.0, 0.0);
		assertThat(resultado.ritmoMedio()).isEqualTo(50.0);
	}

	@Test
	void denominadorFracionarioComSemanalDeMeta3() {
		// diário + semanal meta 3: denominador 1 + 3/7 = 10/7; só o diário feito: 1 / (10/7) = 0,7.
		HabitoHistorico d = diario(1, ANTES, null, D1);
		HabitoHistorico s = semanal(2, ANTES, meta(SEGUNDA_28_09, 3));

		Resultado resultado = RitmoCalculadora.calcular(List.of(d, s), D1, D1);

		assertThat(ritmos(resultado)).containsExactly(70.0);
	}

	@Test
	void ritmoEhLimitadoA100QuandoOsDoisEstaoFeitosNoDiaDeDenominadorFracionario() {
		// 2 conclusões / (10/7) = 1,4: passa de 100% e vira 100%.
		HabitoHistorico d = diario(1, ANTES, null, D1);
		HabitoHistorico s = semanal(2, ANTES, meta(SEGUNDA_28_09, 3), D1);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d, s), D1, D1);

		assertThat(ritmos(resultado)).containsExactly(100.0);
	}

	@Test
	void umaConclusaoAMaisQueOEsperadoNaoPassaDe100() {
		// só um semanal de meta 1 (1/7 por dia) feito no dia: 1 / (1/7) = 7, limitado a 100%.
		Resultado resultado = RitmoCalculadora.calcular(
				List.of(semanal(1, ANTES, meta(SEGUNDA_28_09, 1), D1)), D1, D1);

		assertThat(ritmos(resultado)).containsExactly(100.0);
	}

	@Test
	void semanalSemMetaVigenteNaSemanaNaoContaNemSeTemRegistro() {
		// A primeira meta vale da segunda 05/10: os dias 01 a 04/10 (semana de 28/09) ficam sem valor.
		LocalDate segunda05 = LocalDate.of(2026, 10, 5);
		HabitoHistorico s = semanal(1, ANTES, meta(segunda05, 7), D1, segunda05);

		Resultado resultado = RitmoCalculadora.calcular(List.of(s), D1, segunda05);

		assertThat(ritmos(resultado)).containsExactly(null, null, null, null, 100.0);
	}

	@Test
	void aMetaDaSemanaMudaDeUmaSemanaParaAOutra() {
		// meta 7 até 04/10 e meta 3 a partir da segunda 05/10. Com um diário feito nos dois dias:
		// 04/10: 7 / (7 + 7) = 50,0 ; 05/10: 7 / (7 + 3) = 70,0.
		LocalDate dom04 = LocalDate.of(2026, 10, 4);
		LocalDate seg05 = LocalDate.of(2026, 10, 5);
		List<MetaVigente> metas = List.of(new MetaVigente(seg05, 3), new MetaVigente(SEGUNDA_28_09, 7));
		HabitoHistorico d = diario(1, ANTES, null, dom04, seg05);
		HabitoHistorico s = semanal(2, ANTES, metas);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d, s), dom04, seg05);

		assertThat(ritmos(resultado)).containsExactly(50.0, 70.0);
	}

	// ---- existência do hábito ----

	@Test
	void diaAnteriorAoDaCriacaoNaoContaEORegistroDeLaEhIgnorado() {
		// criado em 02/10, com um registro em 01/10 (antes de existir): 01/10 fica sem valor.
		HabitoHistorico d = diario(1, D2, null, D1, D2);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d), D1, D3);

		assertThat(ritmos(resultado)).containsExactly(null, 100.0, 0.0);
	}

	@Test
	void diaDaExclusaoNaoContaMasOAnteriorConta() {
		LocalDate d5 = LocalDate.of(2026, 10, 5);
		HabitoHistorico d = diario(1, D2, d5, D2, d5);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d), D1, d5);

		// 01/10 antes da criação; 02/10 feito; 03 e 04 não feitos; 05/10 é o dia da exclusão (e o registro de lá é ignorado).
		assertThat(ritmos(resultado)).containsExactly(null, 100.0, 0.0, 0.0, null);
	}

	@Test
	void diasSemNenhumHabitoExistenteFicamForaDasMedias() {
		HabitoHistorico d = diario(1, D2, null, D2);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d), D1, D3);

		// 01/10 sem valor não puxa a média para baixo: (100 + 0) / 2.
		assertThat(resultado.ritmoMedio()).isEqualTo(50.0);
	}

	@Test
	void habitoExcluidoContaNosDiasEmQueExistia() {
		HabitoHistorico excluido = diario(1, ANTES, LocalDate.of(2026, 10, 3), D1);
		HabitoHistorico ativo = diario(2, ANTES, null, D1, D2, D3);

		Resultado resultado = RitmoCalculadora.calcular(List.of(excluido, ativo), D1, D3);

		// 01/10: 2/2 ; 02/10: 1/2 (o excluído ainda existe e não fez) ; 03/10: 1/1 (o excluído saiu).
		assertThat(ritmos(resultado)).containsExactly(100.0, 50.0, 100.0);
	}

	@Test
	void semNenhumHabitoTudoFicaSemValor() {
		Resultado resultado = RitmoCalculadora.calcular(List.of(), D1, D3);

		assertThat(ritmos(resultado)).containsExactly(null, null, null);
		assertThat(resultado.ritmoMedio()).isNull();
		assertThat(resultado.semanas()).hasSize(1);
		assertThat(resultado.semanas().get(0).ritmo()).isNull();
	}

	// ---- blocos S1 a S4 ----

	@Test
	void intervaloDe31DiasTem4BlocosEOS4AbsorveOsDias29A31() {
		LocalDate d31 = LocalDate.of(2026, 10, 31);
		Set<LocalDate> primeiraSemana = new HashSet<>();
		for (int i = 0; i < 7; i++) {
			primeiraSemana.add(D1.plusDays(i));
		}
		HabitoHistorico d = new HabitoHistorico(1L, "D1", TipoHabito.DIARIO, ANTES, null, List.of(), primeiraSemana);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d), D1, d31);

		assertThat(resultado.dias()).hasSize(31);
		assertThat(resultado.semanas()).containsExactly(
				new BlocoRitmo("S1", D1, LocalDate.of(2026, 10, 7), 100.0),
				new BlocoRitmo("S2", LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 14), 0.0),
				new BlocoRitmo("S3", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 21), 0.0),
				new BlocoRitmo("S4", LocalDate.of(2026, 10, 22), d31, 0.0));
		// 7 dias feitos em 31: 22,58 -> 22,6
		assertThat(resultado.ritmoMedio()).isEqualTo(22.6);
	}

	@Test
	void intervaloDe10DiasTemSoDoisBlocosEOSegundoEhCortadoNoFim() {
		LocalDate d10 = LocalDate.of(2026, 10, 10);

		Resultado resultado = RitmoCalculadora.calcular(List.of(diario(1, ANTES, null, D1)), D1, d10);

		assertThat(resultado.semanas()).extracting(BlocoRitmo::rotulo).containsExactly("S1", "S2");
		assertThat(resultado.semanas().get(1).de()).isEqualTo(LocalDate.of(2026, 10, 8));
		assertThat(resultado.semanas().get(1).ate()).isEqualTo(d10);
	}

	@Test
	void intervaloDeUmDiaTemUmBlocoDeUmDia() {
		Resultado resultado = RitmoCalculadora.calcular(List.of(diario(1, ANTES, null, D1)), D1, D1);

		assertThat(resultado.dias()).hasSize(1);
		assertThat(resultado.semanas()).containsExactly(new BlocoRitmo("S1", D1, D1, 100.0));
		assertThat(resultado.ritmoMedio()).isEqualTo(100.0);
	}

	@Test
	void blocoSemNenhumDiaComValorFicaSemRitmoENaoEntraNaMediaGeral() {
		// Criado em 08/10: o S1 (01 a 07/10) não tem hábito; só o S2 tem valores.
		LocalDate d8 = LocalDate.of(2026, 10, 8);
		HabitoHistorico d = diario(1, d8, null, d8);

		Resultado resultado = RitmoCalculadora.calcular(List.of(d), D1, LocalDate.of(2026, 10, 9));

		assertThat(resultado.semanas().get(0).ritmo()).isNull();
		assertThat(resultado.semanas().get(1).ritmo()).isEqualTo(50.0);
		assertThat(resultado.ritmoMedio()).isEqualTo(50.0);
	}

	// ---- arredondamento ----

	@Test
	void arredondaParaUmaCasaDecimalComMeioParaCima() {
		// 1 feito entre 16 diários = 6,25 -> 6,3 ; 1 entre 3 = 33,3 ; 2 entre 3 = 66,7.
		List<HabitoHistorico> dezesseis = new ArrayList<>();
		for (int i = 1; i <= 16; i++) {
			dezesseis.add(diario(i, ANTES, null, i == 1 ? new LocalDate[] { D1 } : new LocalDate[0]));
		}
		List<HabitoHistorico> tres = List.of(diario(1, ANTES, null, D1), diario(2, ANTES, null, D1), diario(3, ANTES, null));

		assertThat(ritmos(RitmoCalculadora.calcular(dezesseis, D1, D1))).containsExactly(6.3);
		assertThat(ritmos(RitmoCalculadora.calcular(tres, D1, D1))).containsExactly(66.7);
		assertThat(ritmos(RitmoCalculadora.calcular(List.of(diario(1, ANTES, null, D1), diario(2, ANTES, null),
				diario(3, ANTES, null)), D1, D1))).containsExactly(33.3);
	}

	@Test
	void asMediasUsamOsValoresSemArredondarPrimeiro() {
		// Dias com 33,33...%, 33,33...% e 100%: a média é 55,55...%, não a média de 33,3, 33,3 e 100 (55,5).
		HabitoHistorico a = diario(1, ANTES, null, D1, D2, D3);
		HabitoHistorico b = diario(2, ANTES, null, D3);
		HabitoHistorico c = diario(3, ANTES, null, D3);

		Resultado resultado = RitmoCalculadora.calcular(List.of(a, b, c), D1, D3);

		assertThat(ritmos(resultado)).containsExactly(33.3, 33.3, 100.0);
		assertThat(resultado.ritmoMedio()).isEqualTo(55.6);
	}

}
