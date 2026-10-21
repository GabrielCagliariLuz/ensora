package com.ensora.evento.dto;

import com.ensora.evento.CategoriaEvento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Corpo de criar e de editar (o PUT substitui o evento inteiro: o que vier ausente é limpo). {@code termino},
 * {@code local} e {@code categoria} são opcionais; sem categoria, o app usa a cor neutra.
 */
public record EventoRequest(
		@NotBlank @Size(max = 150) String titulo,
		@NotNull LocalDate data,
		@NotNull LocalTime inicio,
		LocalTime termino,
		@Size(max = 100) String local,
		CategoriaEvento categoria) {
}
