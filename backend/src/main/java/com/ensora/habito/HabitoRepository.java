package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HabitoRepository extends JpaRepository<Habito, Long> {

	/** O hábito só é achado se for do usuário e não estiver excluído: o filtro por dono mora aqui. */
	Optional<Habito> findByIdAndUsuarioIdAndExcluidoEmIsNull(Long id, Long usuarioId);

	/** Hábitos do usuário que não foram excluídos, na ordem em que foram criados. */
	List<Habito> findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(Long usuarioId);

}
