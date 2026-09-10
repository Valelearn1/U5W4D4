package com.example.demo.websocket;

import com.example.demo.security.JwtHandshakeInterceptor;
import com.example.demo.security.UtenteAutenticato;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.UUID;

/**
 * Canale su cui il client resta in ascolto delle proprie notifiche:
 *   ws://localhost:8080/ws/notifiche?token=<JWT>[&topic=<uuid-canale>]
 *
 * L'identita' NON arriva piu' da un parametro scelto dal client: la stabilisce
 * JwtHandshakeInterceptor verificando la firma del token, prima ancora che la
 * sessione venga aperta.
 */
@Component
public class NotificaWebSocketHandler extends TextWebSocketHandler {

	private final WebSocketSessionRegistry registry;

	public NotificaWebSocketHandler(WebSocketSessionRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		Object attributo = session.getAttributes().get(JwtHandshakeInterceptor.ATTR_UTENTE);
		if (!(attributo instanceof UtenteAutenticato utente)) {
			// non dovrebbe accadere: senza token l'handshake e' gia' stato respinto
			session.close(CloseStatus.POLICY_VIOLATION.withReason("non autenticato"));
			return;
		}

		session.getAttributes().put(WebSocketSessionRegistry.ATTR_UTENTE_ID, utente.id());
		UUID topic = leggiUuid(session.getUri(), "topic");
		if (topic != null) {
			session.getAttributes().put(WebSocketSessionRegistry.ATTR_TOPIC, topic);
		}
		registry.registra(utente.id(), session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		Object utenteId = session.getAttributes().get(WebSocketSessionRegistry.ATTR_UTENTE_ID);
		if (utenteId instanceof UUID id) {
			registry.rimuovi(id, session);
		}
	}

	private static UUID leggiUuid(URI uri, String nomeParametro) {
		if (uri == null || uri.getQuery() == null) {
			return null;
		}
		for (String coppia : uri.getQuery().split("&")) {
			String[] parti = coppia.split("=", 2);
			if (parti.length == 2 && parti[0].equals(nomeParametro)) {
				try {
					return UUID.fromString(parti[1]);
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}
}
