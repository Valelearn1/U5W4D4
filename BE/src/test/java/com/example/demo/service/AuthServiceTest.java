package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegistrazioneRequest;
import com.example.demo.dto.UtenteResponse;
import com.example.demo.enums.Ruolo;
import com.example.demo.repository.UtenteRepository;
import com.example.demo.security.JwtService;
import com.example.demo.security.UtenteAutenticato;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthServiceTest {

	@Autowired AuthService authService;
	@Autowired UtenteRepository utenteRepository;
	@Autowired JwtService jwtService;

	private String nomeUnico() {
		return "utente-" + UUID.randomUUID();
	}

	@Test
	@DisplayName("Registrazione: la password finisce sul DB solo come hash BCrypt")
	void passwordHashata() {
		String username = nomeUnico();
		UtenteResponse creato = authService.registra(new RegistrazioneRequest(username, "password123"));

		var salvato = utenteRepository.findById(creato.id()).orElseThrow();
		assertThat(salvato.getPassword())
				.isNotEqualTo("password123")     // mai in chiaro
				.startsWith("$2a$")              // formato BCrypt
				.hasSize(60);
		assertThat(creato.ruolo()).isEqualTo(Ruolo.USER);   // mai ADMIN da registrazione
	}

	@Test
	@DisplayName("Registrazione: username duplicato rifiutato")
	void usernameDuplicato() {
		String username = nomeUnico();
		authService.registra(new RegistrazioneRequest(username, "password123"));

		assertThatThrownBy(() -> authService.registra(new RegistrazioneRequest(username, "password456")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("gia'");
	}

	@Test
	@DisplayName("Registrazione: validazione di username e password")
	void validazioneRegistrazione() {
		assertThatThrownBy(() -> authService.registra(new RegistrazioneRequest("  ", "password123")))
				.hasMessageContaining("username");
		assertThatThrownBy(() -> authService.registra(new RegistrazioneRequest(nomeUnico(), "corta")))
				.hasMessageContaining("8 caratteri");
	}

	@Test
	@DisplayName("Login corretto: restituisce un token leggibile e coerente")
	void loginRiuscito() {
		String username = nomeUnico();
		UtenteResponse creato = authService.registra(new RegistrazioneRequest(username, "password123"));

		LoginResponse risposta = authService.login(new LoginRequest(username, "password123"));

		assertThat(risposta.tipo()).isEqualTo("Bearer");
		assertThat(risposta.token()).isNotBlank();

		UtenteAutenticato dalToken = jwtService.leggi(risposta.token());
		assertThat(dalToken.id()).isEqualTo(creato.id());
		assertThat(dalToken.username()).isEqualTo(username);
		assertThat(dalToken.ruolo()).isEqualTo(Ruolo.USER);
	}

	@Test
	@DisplayName("Login: password errata e utente inesistente danno lo stesso errore")
	void loginFallito() {
		String username = nomeUnico();
		authService.registra(new RegistrazioneRequest(username, "password123"));

		assertThatThrownBy(() -> authService.login(new LoginRequest(username, "sbagliata")))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("credenziali non valide");

		assertThatThrownBy(() -> authService.login(new LoginRequest("non-esisto", "password123")))
				.isInstanceOf(BadCredentialsException.class)
				.hasMessage("credenziali non valide");   // messaggio identico: non rivela nulla
	}

	@Test
	@DisplayName("Token manomesso: la firma non torna e viene rifiutato")
	void tokenManomesso() {
		String username = nomeUnico();
		authService.registra(new RegistrazioneRequest(username, "password123"));
		String token = authService.login(new LoginRequest(username, "password123")).token();

		// cambio un carattere della firma
		String manomesso = token.substring(0, token.length() - 2)
				+ (token.endsWith("A") ? "B" : "A");

		assertThatThrownBy(() -> jwtService.leggi(manomesso)).isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> jwtService.leggi("non.e.un.token")).isInstanceOf(RuntimeException.class);
	}
}
