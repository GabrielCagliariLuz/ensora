package com.ensora.entrega;

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
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
public class Entrega {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long usuarioId;

	@Column(nullable = false)
	private String titulo;

	@Column(nullable = false)
	private LocalDate prazoData;

	// Nulo quando o usuário não informa; o app trata como 23:59 da data (seção 8).
	private LocalTime prazoHorario;

	@Column(nullable = false)
	private boolean concluida;

	// Preenchido ao concluir e limpo ao desfazer; o banco exige que andem juntos com `concluida`.
	private Instant concluidaEm;

	// O dia da conclusão no fuso do usuário, enviado pelo cliente (concluida_em é UTC): a Home usa esta data para
	// esconder a entrega concluída no dia seguinte, sem fazer conta de fuso no servidor.
	private LocalDate concluidaData;

	private String nota;

	@Generated(event = EventType.INSERT)
	@Column(insertable = false, updatable = false)
	private Instant criadoEm;

}
