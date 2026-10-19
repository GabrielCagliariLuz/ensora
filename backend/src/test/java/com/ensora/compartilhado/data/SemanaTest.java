package com.ensora.compartilhado.data;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SemanaTest {

	// 19/10/2026 é segunda-feira; 25/10/2026 é domingo.
	@ParameterizedTest(name = "{0} cai na semana que começa em {1}")
	@CsvSource({
			"2026-10-19, 2026-10-19",
			"2026-10-20, 2026-10-19",
			"2026-10-21, 2026-10-19",
			"2026-10-22, 2026-10-19",
			"2026-10-23, 2026-10-19",
			"2026-10-24, 2026-10-19",
			"2026-10-25, 2026-10-19",
			"2026-10-26, 2026-10-26"
	})
	void segundaFeiraDaSemanaDeQualquerDia(LocalDate data, LocalDate segundaEsperada) {
		assertThat(Semana.segundaFeiraDe(data)).isEqualTo(segundaEsperada);
	}

	@ParameterizedTest(name = "{0} cai na semana que começa em {1}")
	@CsvSource({
			"2026-12-31, 2026-12-28",
			"2027-01-01, 2026-12-28",
			"2027-01-04, 2027-01-04"
	})
	void semanaQueAtravessaOAnoContinuaSendoDeSegundaADomingo(LocalDate data, LocalDate segundaEsperada) {
		assertThat(Semana.segundaFeiraDe(data)).isEqualTo(segundaEsperada);
	}

}
