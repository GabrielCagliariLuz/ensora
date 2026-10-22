package com.ensora.compartilhado.data;

import com.ensora.compartilhado.erro.ValidacaoException;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FusoHorarioTest {

	@Test
	void aceitaNomeDeRegiaoIana() {
		assertThat(FusoHorario.interpretar("America/Sao_Paulo")).isEqualTo(ZoneId.of("America/Sao_Paulo"));
		assertThat(FusoHorario.interpretar("America/Rio_Branco")).isEqualTo(ZoneId.of("America/Rio_Branco"));
	}

	@Test
	void ignoraEspacosNasPontas() {
		assertThat(FusoHorario.interpretar("  America/Manaus ")).isEqualTo(ZoneId.of("America/Manaus"));
	}

	@Test
	void recusaNomeQueNaoExiste() {
		assertThatThrownBy(() -> FusoHorario.interpretar("Marte/Olimpo"))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> {
					assertThat(erro.getCampo()).isEqualTo("fuso");
					assertThat(erro.getMessage()).isEqualTo("fuso horário inválido");
				});
	}

	@Test
	void recusaDeslocamentoSolto() {
		assertThatThrownBy(() -> FusoHorario.interpretar("+03:00")).isInstanceOf(ValidacaoException.class);
		assertThatThrownBy(() -> FusoHorario.interpretar("UTC-3")).isInstanceOf(ValidacaoException.class);
	}

	@Test
	void recusaVazioENulo() {
		assertThatThrownBy(() -> FusoHorario.interpretar("")).isInstanceOf(ValidacaoException.class);
		assertThatThrownBy(() -> FusoHorario.interpretar(null)).isInstanceOf(ValidacaoException.class);
	}

}
