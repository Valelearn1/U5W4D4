package com.example.demo.security;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Autentica l'handshake WebSocket prima che la connessione venga aperta.
 * Se il token manca o non e' valido, l'handshake si chiude con 401 e la sessione
 * non nasce nemmeno.
 *
 * Perche' il token sta nella query invece che nell'header Authorization: l'API
 * WebSocket dei browser non permette di impostare header sulla richiesta di
 * handshake, quindi la query e' la strada obbligata lato web. Ha un costo noto -
 * gli URL finiscono nei log dei server e dei proxy - che si mitiga tenendo i token
 * a vita breve (qui 60 minuti).
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	public static final String ATTR_UTENTE = "utenteAutenticato";

	private final JwtService jwtService;

	public JwtHandshakeInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
	                               ServerHttpResponse response,
	                               WebSocketHandler wsHandler,
	                               Map<String, Object> attributes) {
		String token = UriComponentsBuilder.fromUri(request.getURI())
				.build().getQueryParams().getFirst("token");

		if (token == null || token.isBlank()) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
		try {
			attributes.put(ATTR_UTENTE, jwtService.leggi(token));
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			response.setStatusCode(HttpStatus.UNAUTHORIZED);
			return false;
		}
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
	                           ServerHttpResponse response,
	                           WebSocketHandler wsHandler,
	                           Exception exception) {
		// niente da fare
	}
}
