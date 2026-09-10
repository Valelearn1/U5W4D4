package com.example.demo.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handler di prova: rimanda al mittente ogni messaggio ricevuto ("echo").
 * Serve a verificare che il canale WebSocket sia attivo end-to-end.
 */
@Component
public class EchoWebSocketHandler extends TextWebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		System.out.println("[WS] connessione aperta: " + session.getId());
		session.sendMessage(new TextMessage("benvenuto, sessione " + session.getId()));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		System.out.println("[WS] ricevuto: " + message.getPayload());
		session.sendMessage(new TextMessage("echo: " + message.getPayload()));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		System.out.println("[WS] connessione chiusa: " + session.getId() + " (" + status + ")");
	}
}
