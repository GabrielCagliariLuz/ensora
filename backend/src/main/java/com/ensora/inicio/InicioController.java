package com.ensora.inicio;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.inicio.dto.InicioResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/inicio")
public class InicioController {

	private final InicioService inicioService;

	public InicioController(InicioService inicioService) {
		this.inicioService = inicioService;
	}

	@GetMapping
	public InicioResponse carregar(@UsuarioLogado Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
		return inicioService.carregar(usuarioId, data);
	}

}
