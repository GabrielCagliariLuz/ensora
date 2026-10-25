package com.ensora.conta;

import com.ensora.auth.LimitadorDeTentativasLogin;
import com.ensora.auth.Usuario;
import com.ensora.auth.UsuarioService;
import com.ensora.compartilhado.erro.LimiteExcedidoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import com.ensora.conta.dto.ContaResponse;
import com.ensora.entrega.EntregaService;
import com.ensora.evento.EventoService;
import com.ensora.habito.HabitoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

	private static final Long USUARIO = 7L;
	private static final String IP = "203.0.113.7";

	@Mock
	private UsuarioService usuarioService;

	@Mock
	private HabitoService habitoService;

	@Mock
	private EntregaService entregaService;

	@Mock
	private EventoService eventoService;

	// O limitador é de verdade (não simulado) para os testes provarem o bloqueio de ponta a ponta.
	private final LimitadorDeTentativasLogin limitador = new LimitadorDeTentativasLogin(
			Clock.fixed(Instant.parse("2026-10-17T12:00:00Z"), ZoneOffset.UTC), 5, 15);

	private ContaService servico() {
		return new ContaService(usuarioService, habitoService, entregaService, eventoService, limitador);
	}

	private Usuario usuario() {
		Usuario usuario = new Usuario();
		usuario.setId(USUARIO);
		usuario.setEmail("a@b.com");
		usuario.setFuso("America/Manaus");
		return usuario;
	}

	@Test
	void buscarDevolveIdEmailEFuso() {
		when(usuarioService.buscar(USUARIO)).thenReturn(usuario());

		ContaResponse conta = servico().buscar(USUARIO);

		assertThat(conta).isEqualTo(new ContaResponse(7L, "a@b.com", "America/Manaus", false));
	}

	@Test
	void concluirIntroducaoDevolveAContaComAIntroducaoConcluida() {
		Usuario concluido = usuario();
		concluido.setIntroducaoConcluidaEm(Instant.parse("2026-10-24T12:00:00Z"));
		when(usuarioService.concluirIntroducao(USUARIO)).thenReturn(concluido);

		ContaResponse conta = servico().concluirIntroducao(USUARIO);

		assertThat(conta.introducaoConcluida()).isTrue();
	}

	@Test
	void alterarFusoGravaOFusoValidoEDevolveAConta() {
		Usuario alterado = usuario();
		alterado.setFuso("Asia/Tokyo");
		when(usuarioService.alterarFuso(USUARIO, ZoneId.of("Asia/Tokyo"))).thenReturn(alterado);

		ContaResponse conta = servico().alterarFuso(USUARIO, " Asia/Tokyo ");

		assertThat(conta.fuso()).isEqualTo("Asia/Tokyo");
	}

	@Test
	void alterarFusoInvalidoLancaValidacaoNoCampoFusoENaoGrava() {
		assertThatThrownBy(() -> servico().alterarFuso(USUARIO, "Marte/Olimpo"))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> assertThat(erro.getCampo()).isEqualTo("fuso"));
		verifyNoInteractions(usuarioService);
	}

	@Test
	void alterarSenhaComASenhaAtualCertaTrocaPelaNova() {
		when(usuarioService.senhaConfere(USUARIO, "senha-antiga")).thenReturn(true);
		when(usuarioService.senhaConfere(USUARIO, "senha-nova-123")).thenReturn(false);

		servico().alterarSenha(USUARIO, "senha-antiga", "senha-nova-123", IP);

		verify(usuarioService).alterarSenha(USUARIO, "senha-nova-123");
	}

	@Test
	void alterarSenhaComSenhaAtualErradaLancaValidacaoNoCampoSenhaAtualENaoTroca() {
		when(usuarioService.senhaConfere(USUARIO, "errada")).thenReturn(false);

		assertThatThrownBy(() -> servico().alterarSenha(USUARIO, "errada", "senha-nova-123", IP))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> {
					assertThat(erro.getCampo()).isEqualTo("senhaAtual");
					assertThat(erro.getMessage()).isEqualTo("senha atual incorreta");
				});
		verify(usuarioService, never()).alterarSenha(any(), any());
	}

	@Test
	void alterarSenhaParaAMesmaSenhaLancaValidacaoNoCampoNovaSenha() {
		when(usuarioService.senhaConfere(USUARIO, "senha-atual-1")).thenReturn(true);

		assertThatThrownBy(() -> servico().alterarSenha(USUARIO, "senha-atual-1", "senha-atual-1", IP))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> assertThat(erro.getCampo()).isEqualTo("novaSenha"));
		verify(usuarioService, never()).alterarSenha(any(), any());
	}

	@Test
	void excluirComASenhaCertaApagaOsDadosNaOrdemEDepoisAConta() {
		when(usuarioService.senhaConfere(USUARIO, "senha-segura")).thenReturn(true);

		servico().excluir(USUARIO, "senha-segura", IP);

		InOrder ordem = inOrder(habitoService, entregaService, eventoService, usuarioService);
		ordem.verify(habitoService).apagarTodosDoUsuario(USUARIO);
		ordem.verify(entregaService).apagarTodasDoUsuario(USUARIO);
		ordem.verify(eventoService).apagarTodosDoUsuario(USUARIO);
		ordem.verify(usuarioService).excluir(USUARIO);
	}

	@Test
	void excluirComSenhaErradaNaoApagaNada() {
		when(usuarioService.senhaConfere(USUARIO, "errada")).thenReturn(false);

		assertThatThrownBy(() -> servico().excluir(USUARIO, "errada", IP))
				.isInstanceOfSatisfying(ValidacaoException.class, erro -> assertThat(erro.getCampo()).isEqualTo("senha"));
		verifyNoInteractions(habitoService, entregaService, eventoService);
		verify(usuarioService, never()).excluir(any());
	}

	@Test
	void chutarASenhaMuitasVezesBloqueiaOIpComOMesmoLimiteDoLogin() {
		when(usuarioService.senhaConfere(USUARIO, "errada")).thenReturn(false);
		ContaService servico = servico();

		for (int i = 0; i < 5; i++) {
			assertThatThrownBy(() -> servico.excluir(USUARIO, "errada", IP)).isInstanceOf(ValidacaoException.class);
		}

		assertThatThrownBy(() -> servico.excluir(USUARIO, "errada", IP)).isInstanceOf(LimiteExcedidoException.class);
	}

}
