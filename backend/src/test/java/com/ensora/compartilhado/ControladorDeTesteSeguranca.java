package com.ensora.compartilhado;

import com.ensora.compartilhado.erro.RecursoNaoEncontradoException;
import com.ensora.compartilhado.erro.ValidacaoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class ControladorDeTesteSeguranca {

	record Entrada(@NotBlank String nome) {
	}

	@PostMapping("/habitos/teste-validacao")
	Map<String, String> validar(@Valid @RequestBody Entrada entrada) {
		return Map.of("nome", entrada.nome());
	}

	@GetMapping("/habitos/teste-acesso-negado")
	void acessoNegado() {
		throw new AccessDeniedException("sem permissão");
	}

	@GetMapping("/habitos/teste-nao-encontrado")
	void naoEncontrado() {
		throw new RecursoNaoEncontradoException("Hábito não encontrado");
	}

	@GetMapping("/habitos/teste-regra-violada")
	void regraViolada() {
		throw new ValidacaoException("vezesPorSemana", "obrigatória para hábito semanal");
	}

	@GetMapping("/auth/teste")
	Map<String, String> publico() {
		return Map.of("status", "ok");
	}

}
