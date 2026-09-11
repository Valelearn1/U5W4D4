package com.example.demo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

/**
 * Traduce le eccezioni dei service in codici HTTP, una volta sola per tutti i controller.
 * Cosi' i service restano ignari del protocollo: sollevano eccezioni di dominio e basta.
 *
 * Estende ResponseEntityExceptionHandler: e' quello che porta in dote la gestione
 * corretta delle eccezioni tipizzate di Spring (rotta inesistente -> 404, metodo non
 * ammesso -> 405, JSON malformato -> 400, ResponseStatusException -> il suo stato).
 * Senza, il catch-all su Exception piu' in basso se le mangerebbe tutte, restituendo 500.
 *
 * ProblemDetail e' il formato standard RFC 9457 per gli errori HTTP.
 */
@Slf4j
@RestControllerAdvice
public class GestoreErrori extends ResponseEntityExceptionHandler {

	/** Risorsa inesistente, o non appartenente all'utente che la chiede. */
	@ExceptionHandler(NoSuchElementException.class)
	public ProblemDetail nonTrovato(NoSuchElementException e) {
		return problema(HttpStatus.NOT_FOUND, "Risorsa non trovata", e.getMessage());
	}

	/** Dati della richiesta non validi. */
	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail richiestaNonValida(IllegalArgumentException e) {
		return problema(HttpStatus.BAD_REQUEST, "Richiesta non valida", e.getMessage());
	}

	/** Conflitto con lo stato attuale: p.es. iscrizione gia' esistente. */
	@ExceptionHandler(IllegalStateException.class)
	public ProblemDetail conflitto(IllegalStateException e) {
		return problema(HttpStatus.CONFLICT, "Conflitto", e.getMessage());
	}

	/** Login fallito. Messaggio volutamente generico, per non rivelare quali username esistono. */
	@ExceptionHandler(BadCredentialsException.class)
	public ProblemDetail credenzialiNonValide(BadCredentialsException e) {
		return problema(HttpStatus.UNAUTHORIZED, "Credenziali non valide", e.getMessage());
	}

	/** Rete di sicurezza: solo cio' che non e' gia' gestito sopra o dalla classe padre. */
	@ExceptionHandler(Exception.class)
	public ProblemDetail erroreImprevisto(Exception e) {
		log.error("errore non gestito", e);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno",
				"si e' verificato un errore imprevisto");
	}

	private static ProblemDetail problema(HttpStatus stato, String titolo, String dettaglio) {
		ProblemDetail p = ProblemDetail.forStatusAndDetail(stato, dettaglio == null ? titolo : dettaglio);
		p.setTitle(titolo);
		return p;
	}
}
