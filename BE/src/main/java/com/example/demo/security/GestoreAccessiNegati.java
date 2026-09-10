package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Risposte per i due casi che Spring Security gestisce nella catena di filtri,
 * fuori dalla portata di @RestControllerAdvice.
 *
 * 401 Unauthorized -> "non so chi sei": manca il token o non e' valido.
 * 403 Forbidden    -> "so chi sei, ma non ti e' permesso": token valido, ruolo insufficiente.
 *
 * Senza questa configurazione Spring userebbe Http403ForbiddenEntryPoint e
 * risponderebbe 403 anche a chi non ha alcun token: il frontend non saprebbe
 * distinguere "rifai il login" da "non hai i permessi".
 */
@Component
public class GestoreAccessiNegati implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public GestoreAccessiNegati(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** Nessuna autenticazione valida. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
	                     AuthenticationException authException) throws IOException {
		scrivi(request, response, HttpStatus.UNAUTHORIZED,
				"Non autenticato", "token mancante, scaduto o non valido");
	}

	/** Autenticato ma senza i permessi necessari. */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
	                   AccessDeniedException accessDeniedException) throws IOException {
		scrivi(request, response, HttpStatus.FORBIDDEN,
				"Accesso negato", "permessi insufficienti per questa operazione");
	}

	private void scrivi(HttpServletRequest request, HttpServletResponse response,
	                    HttpStatus stato, String titolo, String dettaglio) throws IOException {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(stato, dettaglio);
		problema.setTitle(titolo);
		problema.setInstance(java.net.URI.create(request.getRequestURI()));

		response.setStatus(stato.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write(objectMapper.writeValueAsString(problema));
	}
}
