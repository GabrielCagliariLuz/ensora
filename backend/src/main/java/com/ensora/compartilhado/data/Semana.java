package com.ensora.compartilhado.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** A semana do Ensora começa na segunda-feira (decisão 15). */
public final class Semana {

	private Semana() {
	}

	/** Segunda-feira da semana da data. Se a data já é segunda, devolve a própria data. */
	public static LocalDate segundaFeiraDe(LocalDate data) {
		return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

}
