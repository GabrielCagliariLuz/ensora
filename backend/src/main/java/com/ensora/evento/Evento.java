package com.ensora.evento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
public class Evento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long usuarioId;

	@Column(nullable = false)
	private String titulo;

	@Column(nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private LocalTime inicio;

	// Quando existe, é maior que o início (o banco confere); o evento não atravessa a meia-noite.
	private LocalTime termino;

	private String local;

	// Nulo quer dizer sem categoria: o app usa a cor neutra.
	@Enumerated(EnumType.STRING)
	private CategoriaEvento categoria;

	@Generated(event = EventType.INSERT)
	@Column(insertable = false, updatable = false)
	private Instant criadoEm;

}
