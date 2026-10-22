package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

public interface HabitoRepository extends JpaRepository<Habito, Long> {

	/** O hábito só é achado se for do usuário e não estiver excluído: o filtro por dono mora aqui. */
	Optional<Habito> findByIdAndUsuarioIdAndExcluidoEmIsNull(Long id, Long usuarioId);

	/** Todos os hábitos do usuário, inclusive os excluídos: os Insights históricos precisam deles. */
	List<Habito> findByUsuarioIdOrderByCriadoEmAscIdAsc(Long usuarioId);

	/** Hábitos do usuário que não foram excluídos, na ordem em que foram criados. */
	List<Habito> findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(Long usuarioId);

	/** Apaga todos os hábitos do usuário, inclusive os excluídos (exclusão da conta). */
	@Modifying
	@Query("delete from Habito h where h.usuarioId = :usuarioId")
	int apagarDoUsuario(@Param("usuarioId") Long usuarioId);

}
