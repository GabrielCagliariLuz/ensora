package com.ensora.evento;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.evento.dto.EventoRequest;
import com.ensora.evento.dto.EventoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventos")
public class EventoController {

	private final EventoService eventoService;

	public EventoController(EventoService eventoService) {
		this.eventoService = eventoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public EventoResponse criar(@UsuarioLogado Long usuarioId, @Valid @RequestBody EventoRequest requisicao) {
		return eventoService.criar(usuarioId, requisicao);
	}

	@PutMapping("/{id}")
	public EventoResponse atualizar(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@Valid @RequestBody EventoRequest requisicao) {
		return eventoService.atualizar(usuarioId, id, requisicao);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@UsuarioLogado Long usuarioId, @PathVariable Long id) {
		eventoService.excluir(usuarioId, id);
	}

}
