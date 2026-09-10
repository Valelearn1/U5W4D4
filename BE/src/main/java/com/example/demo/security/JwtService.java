package com.example.demo.security;

import com.example.demo.entity.Utente;
import com.example.demo.enums.Ruolo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Genera e verifica i token JWT.
 *
 * Un JWT e' fatto di tre parti separate da punti: intestazione, contenuto e firma.
 * Le prime due sono solo codificate in Base64, NON cifrate: chiunque puo' leggerle.
 * Non ci si mette dentro nulla di segreto. Cio' che garantisce il token e' la FIRMA:
 * senza la chiave del server non e' possibile fabbricarne uno valido ne' alterarne uno.
 */
@Service
public class JwtService {

	private final SecretKey chiave;
	private final Duration durata;

	public JwtService(@Value("${app.jwt.secret}") String secret,
	                  @Value("${app.jwt.durata-minuti}") long durataMinuti) {
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			throw new IllegalStateException(
					"app.jwt.secret troppo corto: servono almeno 32 caratteri per HMAC-SHA256");
		}
		this.chiave = Keys.hmacShaKeyFor(bytes);
		this.durata = Duration.ofMinutes(durataMinuti);
	}

	public String genera(Utente utente) {
		Instant adesso = Instant.now();
		return Jwts.builder()
				.subject(utente.getId().toString())
				.claim("username", utente.getUsername())
				.claim("ruolo", utente.getRuolo().name())
				.issuedAt(Date.from(adesso))
				.expiration(Date.from(adesso.plus(durata)))
				.signWith(chiave)
				.compact();
	}

	/**
	 * @return l'utente contenuto nel token
	 * @throws JwtException se la firma non torna, il token e' scaduto o e' malformato
	 */
	public UtenteAutenticato leggi(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(chiave)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		return new UtenteAutenticato(
				UUID.fromString(claims.getSubject()),
				claims.get("username", String.class),
				Ruolo.valueOf(claims.get("ruolo", String.class)));
	}

	public long durataInSecondi() {
		return durata.toSeconds();
	}
}
