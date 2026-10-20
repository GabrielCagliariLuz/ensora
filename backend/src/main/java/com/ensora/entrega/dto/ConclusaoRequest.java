package com.ensora.entrega.dto;

import jakarta.validation.constraints.NotNull;

/** Corpo do PATCH: {@code true} conclui a entrega e {@code false} desfaz a conclusão. */
public record ConclusaoRequest(@NotNull Boolean concluida) {
}
