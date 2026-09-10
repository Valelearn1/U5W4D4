package com.example.demo.dto;

import com.example.demo.enums.TipoNotifica;

import java.util.UUID;

/**
 * Corpo della POST di creazione.
 *  - PERSONAL -> serve idDestinatario, idCanale deve restare null
 *  - CANALE   -> serve idCanale, i destinatari si ricavano dalle iscrizioni
 *  - ALL      -> nessuno dei due, va a tutti gli utenti
 */
public record NuovaNotificaRequest(
		TipoNotifica tipo,
		UUID idDestinatario,
		UUID idCanale,
		String message
) {
}
