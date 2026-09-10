package com.example.demo.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiene traccia delle sessioni WebSocket aperte, indicizzate per utente.
 * Un utente puo' averne piu' di una (piu' schede del browser, o piu' canali aperti).
 *
 * E' una mappa in memoria: vale per una sola istanza dell'applicazione. Se un giorno
 * l'app girasse su piu' nodi servirebbe un broker esterno (es. Redis), perche' il
 * destinatario potrebbe essere connesso a un nodo diverso da quello che crea la notifica.
 */
@Component
public class WebSocketSessionRegistry {

	public static final String ATTR_UTENTE_ID = "utenteId";
	public static final String ATTR_TOPIC = "topic";

	private final Map<UUID, Set<WebSocketSession>> sessioniPerUtente = new ConcurrentHashMap<>();

	public void registra(UUID utenteId, WebSocketSession session) {
		sessioniPerUtente
				.computeIfAbsent(utenteId, k -> ConcurrentHashMap.newKeySet())
				.add(session);
	}

	public void rimuovi(UUID utenteId, WebSocketSession session) {
		sessioniPerUtente.computeIfPresent(utenteId, (k, set) -> {
			set.remove(session);
			return set.isEmpty() ? null : set;
		});
	}

	public Set<WebSocketSession> sessioniDi(UUID utenteId) {
		return sessioniPerUtente.getOrDefault(utenteId, Set.of());
	}

	public int totaleUtentiConnessi() {
		return sessioniPerUtente.size();
	}
}
