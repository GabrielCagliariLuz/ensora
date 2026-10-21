package com.ensora.entrega.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Corpo do PATCH: {@code true} conclui a entrega e {@code false} desfaz a conclusão. Ao concluir, {@code data} é
 * obrigatória: é o dia de hoje no fuso do usuário, lido pelo front do relógio do aparelho. Ao desfazer, é ignorada.
 */
public record ConclusaoRequest(@NotNull Boolean concluida, LocalDate data) {
}
