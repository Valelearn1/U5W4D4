package com.example.demo.repository;

import com.example.demo.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UtenteRepository extends JpaRepository<Utente, UUID> {

	Optional<Utente> findByUsername(String username);

	boolean existsByUsername(String username);

	/** Solo gli ID: serve al fan-out delle notifiche di tipo ALL, senza caricare gli utenti interi. */
	@Query("SELECT u.id FROM Utente u")
	List<UUID> findAllIds();
}
