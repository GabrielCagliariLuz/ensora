package com.ensora.entrega;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

	/** A entrega só é achada se for do usuário: o filtro por dono mora aqui. */
	Optional<Entrega> findByIdAndUsuarioId(Long id, Long usuarioId);

	/**
	 * Entregas da Home: prazo entre {@code desde} e {@code ate}, que ainda não foram concluídas ou foram concluídas
	 * de {@code desde} em diante. Ordem: prazo, horário (sem horário por último, o app o trata como 23:59) e id.
	 */
	@Query("""
			select e from Entrega e
			where e.usuarioId = :usuarioId
			  and e.prazoData between :desde and :ate
			  and (e.concluida = false or e.concluidaData >= :desde)
			order by e.prazoData asc, e.prazoHorario asc nulls last, e.id asc
			""")
	List<Entrega> findParaHome(@Param("usuarioId") Long usuarioId, @Param("desde") LocalDate desde,
			@Param("ate") LocalDate ate);

}
