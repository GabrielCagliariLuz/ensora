package com.ensora.compartilhado.seguranca;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguracaoWebTest {

	@Test
	void aceitaPrefixoVazioOuComBarra() {
		assertThat(new ConfiguracaoWeb("")).isNotNull();
		assertThat(new ConfiguracaoWeb("/api")).isNotNull();
	}

	@Test
	void recusaPrefixoSemBarraOuComCaminhoDeWindows() {
		assertThatThrownBy(() -> new ConfiguracaoWeb("api"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ensora.api.prefixo");
		assertThatThrownBy(() -> new ConfiguracaoWeb("C:/Program Files/Git/api"))
				.isInstanceOf(IllegalStateException.class);
	}
}
