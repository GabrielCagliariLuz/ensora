package com.ensora.habito;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface RegistroHabitoRepository extends JpaRepository<RegistroHabito, Long> {

	/**
	 * Marca o hábito como feito na data. Se já estava marcado, não faz nada e não dá erro: o ON CONFLICT usa a
	 * restrição única (habito_id, data) e é o banco quem decide, de forma atômica, mesmo com dois pedidos
	 * simultâneos. Devolve 1 se criou o registro e 0 se ele já existia.
	 */
	@Modifying
	@Query(value = "INSERT INTO registro_habito (habito_id, data) VALUES (:habitoId, :data) "
			+ "ON CONFLICT (habito_id, data) DO NOTHING", nativeQuery = true)
	int marcar(@Param("habitoId") Long habitoId, @Param("data") LocalDate data);

	/** Registros de vários hábitos num intervalo de datas (inclusive nas duas pontas). */
	List<RegistroHabito> findByHabitoIdInAndDataBetween(Collection<Long> habitoIds, LocalDate de, LocalDate ate);

	/** Apaga o registro da data. Devolve 0 (sem erro) se não havia registro. */
	@Modifying
	@Query("delete from RegistroHabito r where r.habitoId = :habitoId and r.data = :data")
	int desmarcar(@Param("habitoId") Long habitoId, @Param("data") LocalDate data);

}
