package com.ensora.evento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {

	/** O evento só é achado se for do usuário: o filtro por dono mora aqui. */
	Optional<Evento> findByIdAndUsuarioId(Long id, Long usuarioId);

	/** Eventos do usuário entre duas datas (inclusive), em ordem de data, início e id. */
	List<Evento> findByUsuarioIdAndDataBetweenOrderByDataAscInicioAscIdAsc(Long usuarioId, LocalDate de, LocalDate ate);

	/** Apaga todos os eventos do usuário (exclusão da conta). */
	@Modifying
	@Query("delete from Evento e where e.usuarioId = :usuarioId")
	int apagarDoUsuario(@Param("usuarioId") Long usuarioId);

}
