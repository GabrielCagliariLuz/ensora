package com.ensora.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	/** Revoga só se ainda estiver ativo. Devolve 1 se revogou, 0 se outra requisição chegou antes. */
	@Modifying
	@Query("update RefreshToken t set t.revogadoEm = :agora where t.id = :id and t.revogadoEm is null")
	int revogarSeAtivo(@Param("id") Long id, @Param("agora") Instant agora);

	/** Revoga todas as sessões ativas do usuário. */
	@Modifying
	@Query("update RefreshToken t set t.revogadoEm = :agora where t.usuarioId = :usuarioId and t.revogadoEm is null")
	int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId, @Param("agora") Instant agora);

	/** Apaga todas as sessões do usuário, revogadas ou não: a conta está sendo excluída e a chave estrangeira exige. */
	@Modifying
	@Query("delete from RefreshToken t where t.usuarioId = :usuarioId")
	int apagarDoUsuario(@Param("usuarioId") Long usuarioId);

}
