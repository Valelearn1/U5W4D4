package com.example.demo.repository;

import com.example.demo.entity.Iscrizione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IscrizioneRepository extends JpaRepository<Iscrizione, UUID> {

	/** ID degli iscritti a un canale: e' la lista dei destinatari per le notifiche di canale. */
	@Query("SELECT i.utente.id FROM Iscrizione i WHERE i.canale.id = :canaleId")
	List<UUID> findUtenteIdsByCanaleId(@Param("canaleId") UUID canaleId);

	boolean existsByUtenteIdAndCanaleId(UUID utenteId, UUID canaleId);

	Optional<Iscrizione> findByUtenteIdAndCanaleId(UUID utenteId, UUID canaleId);

	long countByCanaleId(UUID canaleId);
}
