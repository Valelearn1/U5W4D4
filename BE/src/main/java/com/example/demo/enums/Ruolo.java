package com.example.demo.enums;

/**
 * USER  -> utente normale: legge le proprie notifiche, segue i canali
 * ADMIN -> puo' inoltre creare notifiche (POST /api/notifiche)
 */
public enum Ruolo {
	USER,
	ADMIN
}
