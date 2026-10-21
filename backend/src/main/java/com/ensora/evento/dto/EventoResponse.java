package com.ensora.evento.dto;

import com.ensora.evento.CategoriaEvento;
import com.ensora.evento.Evento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventoResponse(Long id, String titulo, LocalDate data, LocalTime inicio, LocalTime termino,
		String local, CategoriaEvento categoria) {

	public static EventoResponse de(Evento evento) {
		return new EventoResponse(evento.getId(), evento.getTitulo(), evento.getData(), evento.getInicio(),
				evento.getTermino(), evento.getLocal(), evento.getCategoria());
	}

}
