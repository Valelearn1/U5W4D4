package com.example.demo.dto;

public record LoginResponse(String token, String tipo, long scadenzaSecondi, UtenteResponse utente) {
	public static LoginResponse di(String token, long scadenzaSecondi, UtenteResponse utente) {
		return new LoginResponse(token, "Bearer", scadenzaSecondi, utente);
	}
}
