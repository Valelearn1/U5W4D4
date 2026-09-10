package com.example.demo.dto;

import com.example.demo.entity.Utente;
import com.example.demo.enums.Ruolo;

import java.time.Instant;
import java.util.UUID;

/** Nota: nessun campo password. Non deve mai uscire dal backend, nemmeno hashata. */
public record UtenteResponse(UUID id, String username, Ruolo ruolo, Instant createdAt) {
	public static UtenteResponse da(Utente u) {
		return new UtenteResponse(u.getId(), u.getUsername(), u.getRuolo(), u.getCreatedAt());
	}
}
