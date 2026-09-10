package com.example.demo.config;

import com.example.demo.security.JwtHandshakeInterceptor;
import com.example.demo.websocket.NotificaWebSocketHandler;
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

	private static final String ORIGINE_FRONTEND = "http://localhost:5173";

	private final NotificaWebSocketHandler notificaWebSocketHandler;
	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

	public WebSocketConfig(NotificaWebSocketHandler notificaWebSocketHandler,
	                       JwtHandshakeInterceptor jwtHandshakeInterceptor) {
		this.notificaWebSocketHandler = notificaWebSocketHandler;
		this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		// ws://localhost:8080/ws/notifiche?token=<JWT>[&topic=<uuid-canale>]
		registry.addHandler(notificaWebSocketHandler, "/ws/notifiche")
				.addInterceptors(jwtHandshakeInterceptor)
				.setAllowedOrigins(ORIGINE_FRONTEND);
	}
}
