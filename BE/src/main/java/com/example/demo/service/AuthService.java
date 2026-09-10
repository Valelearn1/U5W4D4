package com.example.demo.service;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.RegistrazioneRequest;
import com.example.demo.dto.UtenteResponse;
import com.example.demo.entity.Utente;
import com.example.demo.enums.Ruolo;
import com.example.demo.repository.UtenteRepository;
import com.example.demo.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final int LUNGHEZZA_MINIMA_PASSWORD = 8;

	private final UtenteRepository utenteRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UtenteRepository utenteRepository,
	                   PasswordEncoder passwordEncoder,
	                   JwtService jwtService) {
		this.utenteRepository = utenteRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@Transactional
	public UtenteResponse registra(RegistrazioneRequest richiesta) {
		String username = richiesta.username() == null ? null : richiesta.username().trim();
		if (username == null || username.isBlank()) {
			throw new IllegalArgumentException("username obbligatorio");
		}
		if (richiesta.password() == null || richiesta.password().length() < LUNGHEZZA_MINIMA_PASSWORD) {
			throw new IllegalArgumentException(
					"password di almeno " + LUNGHEZZA_MINIMA_PASSWORD + " caratteri");
		}
		if (utenteRepository.existsByUsername(username)) {
			throw new IllegalStateException("username gia' in uso");
		}

		Utente utente = new Utente();
		utente.setUsername(username);
		// la password in chiaro finisce qui e non prosegue: sul DB va solo l'hash
		utente.setPassword(passwordEncoder.encode(richiesta.password()));
		utente.setRuolo(Ruolo.USER);

		return UtenteResponse.da(utenteRepository.saveAndFlush(utente));
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest richiesta) {
		// Messaggio identico per utente inesistente e password errata: dire "utente non
		// trovato" rivelerebbe quali username esistono.
		Utente utente = utenteRepository.findByUsername(
						richiesta.username() == null ? "" : richiesta.username().trim())
				.orElseThrow(() -> new BadCredentialsException("credenziali non valide"));

		if (!passwordEncoder.matches(richiesta.password(), utente.getPassword())) {
			throw new BadCredentialsException("credenziali non valide");
		}

		return LoginResponse.di(
				jwtService.genera(utente),
				jwtService.durataInSecondi(),
				UtenteResponse.da(utente));
	}
}
