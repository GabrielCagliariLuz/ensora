package com.ensora.evento;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

	/** O evento só é achado se for do usuário: o filtro por dono mora aqui. */
	Optional<Evento> findByIdAndUsuarioId(Long id, Long usuarioId);

	/** Eventos do usuário entre duas datas (inclusive), em ordem de data, início e id. */
	List<Evento> findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(Long usuarioId, LocalDate de, LocalDate ate);

}
