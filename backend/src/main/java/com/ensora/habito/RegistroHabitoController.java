package com.ensora.habito;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/habitos/{id}/registros/{data}")
public class RegistroHabitoController {

	private final RegistroHabitoService registroHabitoService;

	public RegistroHabitoController(RegistroHabitoService registroHabitoService) {
		this.registroHabitoService = registroHabitoService;
	}

	@PutMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void marcar(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		registroHabitoService.marcar(usuarioId, id, data);
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void desmarcar(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		registroHabitoService.desmarcar(usuarioId, id, data);
	}

}
