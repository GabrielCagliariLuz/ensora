package com.ensora.entrega;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {

	/** A entrega só é achada se for do usuário: o filtro por dono mora aqui. */
	Optional<Entrega> findByIdAndUsuarioId(Long id, Long usuarioId);

}
