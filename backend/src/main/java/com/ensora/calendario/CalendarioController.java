package com.ensora.calendario;

import com.ensora.calendario.dto.CalendarioResponse;
import com.ensora.compartilhado.seguranca.UsuarioLogado;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/calendario")
public class CalendarioController {

	private final CalendarioService calendarioService;

	public CalendarioController(CalendarioService calendarioService) {
		this.calendarioService = calendarioService;
	}

	@GetMapping
	public CalendarioResponse carregar(@UsuarioLogado Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return calendarioService.carregar(usuarioId, de, ate);
	}

}
