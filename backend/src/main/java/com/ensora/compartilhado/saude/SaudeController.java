package com.ensora.compartilhado.saude;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check do Render e alvo do monitor de keep-alive. Público e sem banco: chamar o monitor não acorda o Neon
 * (que suspende depois de 5 minutos parado), só o Render.
 */
@RestController
public class SaudeController {

	@GetMapping("/saude")
	public Map<String, String> saude() {
		return Map.of("status", "ok");
	}

}
