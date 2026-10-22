package com.ensora.integracao;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Insights: os hábitos e registros de B não entram na consistência nem no ritmo de A. */
class IsolamentoInsightsIT extends IntegracaoBase {

	// Os Insights só contam a partir do dia em que o hábito foi criado, então os testes usam o dia de hoje (no fuso
	// padrão das contas novas, o mesmo que o servidor usa ao criar o hábito).
	private static final LocalDate HOJE = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
	private static final String CONSISTENCIA = "/insights/consistencia?mes=" + YearMonth.from(HOJE);
	private static final String RITMO = "/insights/ritmo?de=" + HOJE + "&ate=" + HOJE;

	@Test
	void consistenciaERitmoDeUmNaoContamOsRegistrosDoOutro() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		Sessao semDados = novaConta();
		long habitoDeB = criar(b, "/habitos", "{\"nome\":\"Ler\",\"tipo\":\"DIARIO\"}");
		mvc.perform(como(b, put("/habitos/" + habitoDeB + "/registros/" + HOJE))).andExpect(status().isNoContent());

		// Sanidade: B vê o próprio hábito e o ritmo dele já saiu do zero.
		mvc.perform(como(b, get(CONSISTENCIA))).andExpect(jsonPath("$.habitos.length()").value(1));
		assertThat(ler(b, RITMO)).isNotEqualTo(ler(semDados, RITMO));

		mvc.perform(como(a, get(CONSISTENCIA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos").isEmpty());
		// A conta de A é idêntica à de quem nunca registrou nada: o ritmo dela não carrega nada de B.
		assertThat(ler(a, RITMO)).isEqualTo(ler(semDados, RITMO));
	}
}
