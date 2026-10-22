package com.ensora.integracao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Entregas, eventos, Calendar e Home: A não enxerga nem mexe no que é de B, e nada fica marcado em B. */
class IsolamentoAgendaIT extends IntegracaoBase {

	private static final String ENTREGA = "{\"titulo\":\"Relatório\",\"prazoData\":\"2026-10-25\"}";
	private static final String EVENTO = "{\"titulo\":\"Consulta\",\"data\":\"2026-10-25\",\"inicio\":\"09:00\"}";
	private static final String CALENDARIO = "/calendario?de=2026-10-01&ate=2026-10-31";
	private static final String HOME = "/inicio?data=2026-10-22";

	@Test
	void editarConcluirOuExcluirEntregaDeOutroDa404ENadaMuda() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		long entregaDeB = criar(b, "/entregas", ENTREGA);
		String rota = "/entregas/" + entregaDeB;

		mvc.perform(comJson(como(a, put(rota)), "{\"titulo\":\"Invadida\",\"prazoData\":\"2026-10-26\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(comJson(como(a, patch(rota)), "{\"concluida\":true,\"data\":\"2026-10-22\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(como(a, delete(rota))).andExpect(status().isNotFound());

		String calendarioDeB = ler(b, CALENDARIO);
		assertThat(calendarioDeB).contains("\"id\":" + entregaDeB).contains("Relatório")
				.contains("\"concluida\":false").doesNotContain("Invadida");
	}

	@Test
	void editarOuExcluirEventoDeOutroDa404ENadaMuda() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		long eventoDeB = criar(b, "/eventos", EVENTO);
		String rota = "/eventos/" + eventoDeB;

		mvc.perform(comJson(como(a, put(rota)),
				"{\"titulo\":\"Invadido\",\"data\":\"2026-10-26\",\"inicio\":\"10:00\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(como(a, delete(rota))).andExpect(status().isNotFound());

		String calendarioDeB = ler(b, CALENDARIO);
		assertThat(calendarioDeB).contains("\"id\":" + eventoDeB).contains("Consulta").doesNotContain("Invadido");
	}

	@Test
	void calendarioEHomeDeUmNaoTrazemOQueEDoOutro() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		criar(b, "/entregas", ENTREGA);
		criar(b, "/eventos", EVENTO);
		criar(b, "/habitos", "{\"nome\":\"Ler\",\"tipo\":\"DIARIO\"}");

		mvc.perform(como(a, get(CALENDARIO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entregas").isEmpty())
				.andExpect(jsonPath("$.eventos").isEmpty());
		mvc.perform(como(a, get(HOME)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.habitos").isEmpty())
				.andExpect(jsonPath("$.upcoming.entregas").isEmpty())
				.andExpect(jsonPath("$.upcoming.eventos").isEmpty());

		// Sanidade: o dono vê tudo, então o vazio de A vem do filtro e não de dados que não foram gravados.
		mvc.perform(como(b, get(CALENDARIO)))
				.andExpect(jsonPath("$.entregas.length()").value(1))
				.andExpect(jsonPath("$.eventos.length()").value(1));
		mvc.perform(como(b, get(HOME))).andExpect(jsonPath("$.habitos.length()").value(1));
	}
}
