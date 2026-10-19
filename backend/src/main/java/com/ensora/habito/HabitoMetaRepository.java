package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface HabitoMetaRepository extends JpaRepository<HabitoMeta, Long> {

	/** Metas de vários hábitos de uma vez, da vigência mais recente para a mais antiga. */
	List<HabitoMeta> findByHabitoIdInOrderByVigenteDesdeDesc(Collection<Long> habitoIds);

}
