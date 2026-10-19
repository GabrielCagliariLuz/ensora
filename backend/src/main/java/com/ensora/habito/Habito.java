package com.ensora.habito;

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

@Entity
@Table(name = "habito")
@Getter
@Setter
@NoArgsConstructor
public class Habito {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long usuarioId;

	@Column(nullable = false)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private TipoHabito tipo;

	@Generated(event = EventType.INSERT)
	@Column(insertable = false, updatable = false)
	private Instant criadoEm;

	private Instant excluidoEm;

}
