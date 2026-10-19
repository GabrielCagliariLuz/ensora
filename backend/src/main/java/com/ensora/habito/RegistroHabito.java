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
@Table(name = "registro_habito")
@Getter
@Setter
@NoArgsConstructor
public class RegistroHabito {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long habitoId;

	// Data da conclusão, enviada pelo cliente, sem conversão de fuso (seção 6).
	@Column(nullable = false)
	private LocalDate data;

}
