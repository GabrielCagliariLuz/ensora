package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitoRepository extends JpaRepository<Habito, Long> {

	/** Hábitos do usuário que não foram excluídos, na ordem em que foram criados. */
	List<Habito> findByUsuarioIdAndExcluidoEmIsNullOrderByCriadoEmAscIdAsc(Long usuarioId);

}
