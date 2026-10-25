package com.ensora.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String senhaHash;

	// Nome IANA do fuso (ex.: America/Sao_Paulo); define o "hoje" e o dia de criação nos Insights.
	@Column(nullable = false)
	private String fuso = "America/Sao_Paulo";

	// Quando concluiu ou pulou o tutorial de primeiro acesso; nulo enquanto não viu.
	private Instant introducaoConcluidaEm;

	@Generated(event = EventType.INSERT)
	@Column(insertable = false, updatable = false)
	private Instant criadoEm;

}
