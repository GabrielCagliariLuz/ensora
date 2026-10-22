package com.ensora.integracao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Conta: excluir a de A apaga tudo o que é dela e não toca em nada de B; trocar o fuso de A não muda o de B. */
class IsolamentoContaIT extends IntegracaoBase {

	private static final String CALENDARIO = "/calendario?de=2026-10-01&ate=2026-10-31";

	private long semear(Sessao dono, String nome) throws Exception {
		long habito = criar(dono, "/habitos", "{\"nome\":\"" + nome + "\",\"tipo\":\"DIARIO\"}");
		mvc.perform(como(dono, put("/habitos/" + habito + "/registros/2026-10-20"))).andExpect(status().isNoContent());
		criar(dono, "/entregas", "{\"titulo\":\"Entrega " + nome + "\",\"prazoData\":\"2026-10-25\"}");
		criar(dono, "/eventos",
				"{\"titulo\":\"Evento " + nome + "\",\"data\":\"2026-10-25\",\"inicio\":\"09:00\"}");
		return habito;
	}

	@Test
	void excluirContaDeUmApagaOsDadosDelaEPoupaOsDoOutro() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		semear(a, "DoA");
		long habitoDeB = semear(b, "DoB");

		mvc.perform(comJson(como(a, delete("/usuario")), "{\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isNoContent());

		// A conta de A deixou de existir.
		mvc.perform(comJson(post("/auth/login"), "{\"email\":\"" + a.email() + "\",\"senha\":\"" + SENHA + "\"}"))
				.andExpect(status().isUnauthorized());

		// B segue inteiro: conta, hábito com o registro, entrega e evento.
		mvc.perform(como(b, get("/usuario")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(b.email()));
		assertThat(ler(b, "/habitos")).contains("\"id\":" + habitoDeB).contains("DoB");
		assertThat(ler(b, CALENDARIO)).contains("Entrega DoB").contains("Evento DoB");
		mvc.perform(como(b, get("/inicio?data=2026-10-20")))
				.andExpect(jsonPath("$.habitos[0].feito").value(true));
	}

	@Test
	void trocarOFusoDeUmNaoMudaOFusoDoOutro() throws Exception {
		Sessao a = novaConta();
		Sessao b = novaConta();
		String contaDeB = ler(b, "/usuario");

		mvc.perform(comJson(como(a, put("/usuario/fuso")), "{\"fuso\":\"Asia/Tokyo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fuso").value("Asia/Tokyo"));

		assertThat(ler(b, "/usuario")).isEqualTo(contaDeB);
	}
}
