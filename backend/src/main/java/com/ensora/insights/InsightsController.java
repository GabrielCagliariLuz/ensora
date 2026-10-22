package com.ensora.insights;

import com.ensora.compartilhado.seguranca.UsuarioLogado;
import com.ensora.insights.dto.ConsistenciaResponse;
import com.ensora.insights.dto.RitmoResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/insights")
public class InsightsController {

	private final InsightsService insightsService;

	public InsightsController(InsightsService insightsService) {
		this.insightsService = insightsService;
	}

	@GetMapping("/consistencia")
	public ConsistenciaResponse consistencia(@UsuarioLogado Long usuarioId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
		return insightsService.consistencia(usuarioId, mes);
	}

	@GetMapping("/ritmo")
	public RitmoResponse ritmo(@UsuarioLogado Long usuarioId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return insightsService.ritmo(usuarioId, de, ate);
	}

}
