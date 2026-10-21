package com.ensora.entrega.dto;

import com.ensora.entrega.Entrega;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntregaResponse(Long id, String titulo, LocalDate prazoData, LocalTime prazoHorario,
		boolean concluida, Instant concluidaEm, LocalDate concluidaData, String nota) {

	public static EntregaResponse de(Entrega entrega) {
		return new EntregaResponse(entrega.getId(), entrega.getTitulo(), entrega.getPrazoData(),
				entrega.getPrazoHorario(), entrega.isConcluida(), entrega.getConcluidaEm(),
				entrega.getConcluidaData(), entrega.getNota());
	}

}
