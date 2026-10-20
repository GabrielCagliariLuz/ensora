package com.ensora.inicio;

import com.ensora.habito.HabitoService;
import com.ensora.inicio.dto.InicioResponse;
import com.ensora.inicio.dto.InicioResponse.HabitoNaHome;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Leitura agregada da Home: só orquestra Services de outros módulos, nunca Repositories. */
@Service
public class InicioService {

	private final HabitoService habitoService;

	public InicioService(HabitoService habitoService) {
		this.habitoService = habitoService;
	}

	public InicioResponse carregar(Long usuarioId, LocalDate data) {
		return new InicioResponse(data, habitoService.listarDoDia(usuarioId, data).stream()
				.map(HabitoNaHome::de)
				.toList());
	}

}
