package com.example.demo.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * Su ogni richiesta: legge il token dall'header Authorization, lo verifica e
 * deposita l'utente nel SecurityContext.
 *
 * Se il token manca o non e' valido NON blocca qui: si limita a non autenticare.
 * A negare l'accesso ci pensa dopo la SecurityFilterChain, che sa quali rotte
 * sono pubbliche e quali no.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String PREFISSO = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request,
	                                @NonNull HttpServletResponse response,
	                                @NonNull FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(PREFISSO)) {
			try {
				UtenteAutenticato utente = jwtService.leggi(header.substring(PREFISSO.length()));
				var autorita = List.of(new SimpleGrantedAuthority("ROLE_" + utente.ruolo().name()));
				var autenticazione = new UsernamePasswordAuthenticationToken(utente, null, autorita);
				SecurityContextHolder.getContext().setAuthentication(autenticazione);
			} catch (JwtException | IllegalArgumentException e) {
				log.debug("token non valido: {}", e.getMessage());
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
