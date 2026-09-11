package com.example.demo.service;

import com.example.demo.dto.NotificaResponse;
import com.example.demo.entity.Notifica;
import com.example.demo.websocket.WebSocketSessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Consegna immediata via WebSocket, se il destinatario e' connesso.
 * Se non lo e' non succede nulla: la notifica resta sul database e la vedra' al prossimo GetAll.
 */
@Slf4j
@Service
public class NotificaPushService {

	private final WebSocketSessionRegistry registry;
	private final ObjectMapper objectMapper;

	public NotificaPushService(WebSocketSessionRegistry registry, ObjectMapper objectMapper) {
		this.registry = registry;
		this.objectMapper = objectMapper;
	}

	/** @return numero di sessioni raggiunte */
	public int push(Notifica notifica) {
		UUID destinatarioId = notifica.getDestinatario().getId();
		UUID topicNotifica = notifica.getCanale() == null ? null : notifica.getCanale().getId();
		String payload = objectMapper.writeValueAsString(NotificaResponse.da(notifica));

		int inviate = 0;
		for (WebSocketSession session : registry.sessioniDi(destinatarioId)) {
			if (!session.isOpen() || !topicCompatibile(session, topicNotifica)) {
				continue;
			}
			try {
				// una WebSocketSession non e' thread-safe in scrittura
				synchronized (session) {
					session.sendMessage(new TextMessage(payload));
				}
				inviate++;
			} catch (IOException e) {
				log.warn("invio WebSocket fallito per la sessione {}", session.getId(), e);
			}
		}
		return inviate;
	}

	/**
	 * Regole di consegna:
	 *  - notifica di sistema (PERSONAL/ALL) -> a tutte le sessioni dell'utente;
	 *  - sessione SENZA topic ("campanella", vista generale) -> riceve tutto cio' che
	 *    e' indirizzato a lei, comprese le notifiche di canale;
	 *  - sessione CON topic (l'utente sta guardando un canale) -> solo quel canale.
	 *
	 * Il caso della sessione senza topic e' quello che serve al frontend: senza,
	 * per vedere le notifiche di canale in tempo reale servirebbe una connessione
	 * WebSocket per ogni canale seguito. Non c'e' rischio di fuga di dati: il
	 * destinatario della riga e' gia' il filtro principale.
	 */
	private boolean topicCompatibile(WebSocketSession session, UUID topicNotifica) {
		if (topicNotifica == null) {
			return true;
		}
		Object topicSessione = session.getAttributes().get(WebSocketSessionRegistry.ATTR_TOPIC);
		if (topicSessione == null) {
			return true;
		}
		return Objects.equals(topicSessione, topicNotifica);
	}
}
