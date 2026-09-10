package com.example.demo.repository;

import com.example.demo.entity.Canale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CanaleRepository extends JpaRepository<Canale, UUID> {

	/** Tutti i canali, dal piu' recente. Il nome del metodo basta: Spring Data ne deriva la query. */
	Page<Canale> findAllByOrderByCreatedAtDesc(Pageable pageable);

	/**
	 * Solo i canali a cui l'utente e' iscritto: e' la INNER JOIN della specifica,
	 * scritta in JPQL (si ragiona per entita', non per tabelle).
	 */
	@Query(value = """
			SELECT c FROM Canale c
			JOIN Iscrizione i ON i.canale = c
			WHERE i.utente.id = :utenteId
			ORDER BY c.createdAt DESC
			""",
			countQuery = """
			SELECT COUNT(c) FROM Canale c
			JOIN Iscrizione i ON i.canale = c
			WHERE i.utente.id = :utenteId
			""")
	Page<Canale> findIscrittoDa(@Param("utenteId") UUID utenteId, Pageable pageable);
}
