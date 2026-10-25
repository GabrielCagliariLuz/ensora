package com.ensora.conta;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.conta.dto.ContaResponse;
import com.ensora.conta.dto.ExclusaoRequest;
import com.ensora.conta.dto.FusoRequest;
import com.ensora.conta.dto.SenhaRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuario")
public class ContaController {

	private final ContaService contaService;

	public ContaController(ContaService contaService) {
		this.contaService = contaService;
	}

	@GetMapping
	public ContaResponse buscar(@UsuarioLogado Long usuarioId) {
		return contaService.buscar(usuarioId);
	}

	@PutMapping("/fuso")
	public ContaResponse alterarFuso(@UsuarioLogado Long usuarioId, @Valid @RequestBody FusoRequest requisicao) {
		return contaService.alterarFuso(usuarioId, requisicao.fuso());
	}

	@PutMapping("/introducao")
	public ContaResponse concluirIntroducao(@UsuarioLogado Long usuarioId) {
		return contaService.concluirIntroducao(usuarioId);
	}

	@PutMapping("/senha")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void alterarSenha(@UsuarioLogado Long usuarioId, @Valid @RequestBody SenhaRequest requisicao,
			HttpServletRequest http) {
		contaService.alterarSenha(usuarioId, requisicao.senhaAtual(), requisicao.novaSenha(), http.getRemoteAddr());
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@UsuarioLogado Long usuarioId, @Valid @RequestBody ExclusaoRequest requisicao,
			HttpServletRequest http) {
		contaService.excluir(usuarioId, requisicao.senha(), http.getRemoteAddr());
	}

}
