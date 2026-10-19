package com.ensora.habito;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "habito_meta")
@Getter
@Setter
@NoArgsConstructor
public class HabitoMeta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long habitoId;

	@Column(nullable = false)
	private Integer vezesPorSemana;

	// Sempre uma segunda-feira (o banco confere com CHECK).
	@Column(nullable = false)
	private LocalDate vigenteDesde;

}
