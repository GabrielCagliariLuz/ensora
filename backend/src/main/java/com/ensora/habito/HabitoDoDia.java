package com.ensora.habito;

/**
 * Um hábito na visão da Home de um dia. {@code feitos} e {@code meta} só existem no semanal: são a contagem da
 * semana da data e a meta que valia naquela semana (nulos no diário).
 */
public record HabitoDoDia(Long id, String nome, TipoHabito tipo, boolean feito, Integer feitos, Integer meta) {
}
