package com.example.demo.repository;

import com.example.demo.entity.Notifica;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificaRepository extends JpaRepository<Notifica, UUID> {

	/**
	 * Feed dell'utente: prima le NON lette, poi le lette, entrambe dalla piu' recente.
	 * Il CASE WHEN produce 0 per le non lette e 1 per le lette: ordinando per quel
	 * valore crescente, le non lette finiscono in testa.
	 */
	@Query("""
			SELECT n FROM Notifica n
			WHERE n.destinatario.id = :utenteId
			ORDER BY CASE WHEN n.readAt IS NULL THEN 0 ELSE 1 END ASC, n.createdAt DESC
			""")
	Page<Notifica> findFeedByDestinatario(@Param("utenteId") UUID utenteId, Pageable pageable);

	long countByDestinatarioIdAndReadAtIsNull(UUID utenteId);

	/** Cerca la notifica solo se appartiene davvero a quell'utente. */
	Optional<Notifica> findByIdAndDestinatarioId(UUID id, UUID destinatarioId);

	/**
	 * Segna come lette tutte le non lette dell'utente con una sola UPDATE.
	 * clearAutomatically svuota il persistence context: le UPDATE massive scrivono
	 * direttamente sul DB e Hibernate non se ne accorgerebbe, restituendo dati stantii.
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE Notifica n SET n.readAt = :adesso
			WHERE n.destinatario.id = :utenteId AND n.readAt IS NULL
			""")
	int marcaTutteComeLette(@Param("utenteId") UUID utenteId, @Param("adesso") Instant adesso);
}
