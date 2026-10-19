package com.ensora.habito;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.habito.dto.HabitoRequest;
import com.ensora.habito.dto.HabitoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/habitos")
public class HabitoController {

	private final HabitoService habitoService;

	public HabitoController(HabitoService habitoService) {
		this.habitoService = habitoService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public HabitoResponse criar(@UsuarioLogado Long usuarioId, @Valid @RequestBody HabitoRequest requisicao) {
		return habitoService.criar(usuarioId, requisicao);
	}

	@GetMapping
	public List<HabitoResponse> listar(@UsuarioLogado Long usuarioId) {
		return habitoService.listar(usuarioId);
	}

	@PutMapping("/{id}")
	public HabitoResponse atualizar(@UsuarioLogado Long usuarioId, @PathVariable Long id,
			@Valid @RequestBody HabitoRequest requisicao) {
		return habitoService.atualizar(usuarioId, id, requisicao);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(@UsuarioLogado Long usuarioId, @PathVariable Long id) {
		habitoService.excluir(usuarioId, id);
	}

}
