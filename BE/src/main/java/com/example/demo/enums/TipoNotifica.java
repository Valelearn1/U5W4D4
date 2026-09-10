package com.example.demo.enums;

/**
 * PERSONAL -> notifica di sistema indirizzata a un solo utente
 * CANALE   -> notifica indirizzata agli iscritti di un canale (richiede id_canale)
 * ALL      -> notifica di sistema indirizzata a tutti
 */
public enum TipoNotifica {
	PERSONAL,
	CANALE,
	ALL
}
