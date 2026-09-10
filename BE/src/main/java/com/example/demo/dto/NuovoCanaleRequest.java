package com.example.demo.dto;

import java.util.UUID;

/** idCreatore arrivera' dal JWT una volta implementato, non dal corpo della richiesta. */
public record NuovoCanaleRequest(
		String nome,
		String descrizione,
		UUID idCreatore
) {
}
