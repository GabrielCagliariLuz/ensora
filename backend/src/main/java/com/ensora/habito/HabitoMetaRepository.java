package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HabitoMetaRepository extends JpaRepository<HabitoMeta, Long> {

	/** A vigência que começa exatamente nesta data (uma vigência futura que ainda pode ser substituída). */
	Optional<HabitoMeta> findByHabitoIdAndVigenteDesde(Long habitoId, LocalDate vigenteDesde);

	/** A meta que vale numa data: a de maior vigente_desde menor ou igual a ela. */
	Optional<HabitoMeta> findFirstByHabitoIdAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
			Long habitoId, LocalDate data);

	/** Metas de vários hábitos de uma vez, da vigência mais recente para a mais antiga. */
	List<HabitoMeta> findByHabitoIdInOrderByVigenteDesdeDesc(Collection<Long> habitoIds);

	/** Apaga todas as metas dos hábitos do usuário (exclusão da conta). */
	@Modifying
	@Query("delete from HabitoMeta m where m.habitoId in (select h.id from Habito h where h.usuarioId = :usuarioId)")
	int apagarDoUsuario(@Param("usuarioId") Long usuarioId);

}
