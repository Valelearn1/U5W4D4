package com.example.demo.dto;

import com.example.demo.entity.Canale;

import java.time.Instant;
import java.util.UUID;

/**
 * Output previsto dalla specifica (Id, Nome, Descrizione), piu' createdAt:
 * serve al frontend perche' l'elenco e' ordinato proprio per data di creazione.
 */
public record CanaleResponse(
		UUID id,
		String nome,
		String descrizione,
		Instant createdAt
) {
	public static CanaleResponse da(Canale c) {
		return new CanaleResponse(c.getId(), c.getNome(), c.getDescrizione(), c.getCreatedAt());
	}
}
