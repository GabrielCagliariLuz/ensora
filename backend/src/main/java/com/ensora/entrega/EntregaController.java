package com.ensora.entrega;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.entrega.dto.ConclusaoRequest;
import com.ensora.entrega.dto.EntregaRequest;
import com.ensora.entrega.dto.EntregaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

	private final EntregaService entregaService;

	public EntregaController(EntregaService entregaService) {
		this.entregaService = entregaService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EntregaResponse criar(@UsuarioLogado Long usuarioId, @Valid @RequestBody EntregaRequest requisicao) {
		return entregaService.criar(usuarioId, requisicao);
	}

	@PutMapping("/{id}")
	public EntregaResponse atualizar(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@Valid @RequestBody EntregaRequest requisicao) {
		return entregaService.atualizar(usuarioId, id, requisicao);
	}

	@PatchMapping("/{id}")
	public EntregaResponse alterarConclusao(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@Valid @RequestBody ConclusaoRequest requisicao) {
		return entregaService.alterarConclusao(usuarioId, id, requisicao.concluida(), requisicao.data());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@UsuarioLogado Long usuarioId, @PathVariable Long id) {
		entregaService.excluir(usuarioId, id);
	}

}
