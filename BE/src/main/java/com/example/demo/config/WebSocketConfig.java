package com.example.demo.config;

import com.example.demo.websocket.EchoWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registra gli endpoint WebSocket. L'handshake NON passa dal CORS di Spring MVC:
 * le origini ammesse vanno dichiarate qui con setAllowedOrigins.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final EchoWebSocketHandler echoWebSocketHandler;

	public WebSocketConfig(EchoWebSocketHandler echoWebSocketHandler) {
		this.echoWebSocketHandler = echoWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(echoWebSocketHandler, "/ws/echo")
				.setAllowedOrigins("http://localhost:5173");
	}
}
