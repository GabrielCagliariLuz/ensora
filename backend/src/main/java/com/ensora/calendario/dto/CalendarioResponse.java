package com.ensora.calendario.dto;

import com.ensora.entrega.dto.EntregaResponse;
import com.ensora.evento.dto.EventoResponse;

import java.time.LocalDate;
import java.util.List;

/** Entregas e eventos do intervalo, em listas planas; os itens têm o mesmo formato dos CRUDs de entrega e de evento. */
public record CalendarioResponse(LocalDate de, LocalDate ate, List<EntregaResponse> entregas,
		List<EventoResponse> eventos) {
}
