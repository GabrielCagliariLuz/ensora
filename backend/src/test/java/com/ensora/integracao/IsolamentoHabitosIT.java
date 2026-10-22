package com.ensora.integracao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A não enxerga nem mexe nos hábitos e registros de B; e o que A tenta não deixa marca em B. */
class IsolamentoHabitosIT extends IntegracaoBase {

	private static final String DIARIO = "{\"nome\":\"Ler\",\"tipo\":\"DIARIO\"}";

	@Test
	void listaDeUmNaoTrazOsHabitosDoOutro() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		criar(b, "/habitos", DIARIO);

		mvc.perform(como(a, get("/habitos")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void editarOuExcluirHabitoDeOutroDa404ENadaMuda() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		long habitoDeB = criar(b, "/habitos", DIARIO);

		mvc.perform(comJson(como(a, put("/habitos/" + habitoDeB)), "{\"nome\":\"Invadido\",\"tipo\":\"DIARIO\"}"))
				.andExpect(status().isNotFound());
		mvc.perform(como(a, delete("/habitos/" + habitoDeB))).andExpect(status().isNotFound());

		String habitosDeB = ler(b, "/habitos");
		assertThat(habitosDeB).contains("\"id\":" + habitoDeB).contains("Ler")
				.doesNotContain("Invadido");
	}

	@Test
	void marcarEDesmarcarRegistroEmHabitoDeOutroDa404() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		long habitoDeB = criar(b, "/habitos", DIARIO);
		String rota = "/habitos/" + habitoDeB + "/registros/2026-03-10";

		mvc.perform(como(a, put(rota))).andExpect(status().isNotFound());
		mvc.perform(como(a, delete(rota))).andExpect(status().isNotFound());

		// O dono consegue marcar: a rota existe, só não serve a quem não é dono.
		mvc.perform(como(b, put(rota))).andExpect(status().isNoContent());
	}
}
