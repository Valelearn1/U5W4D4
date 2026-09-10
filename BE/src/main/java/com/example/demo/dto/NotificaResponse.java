package com.example.demo.dto;

import com.example.demo.entity.Notifica;
import com.example.demo.enums.TipoNotifica;

import java.time.Instant;
import java.util.UUID;

/**
 * Output previsto dalla specifica (Tipo, ID_Notifica, ID_Canale, Messaggio),
 * piu' createdAt e readAt che servono al frontend per ordinare e per capire
 * quali notifiche mostrare come "da leggere".
 */
public record NotificaResponse(
		UUID id,
		TipoNotifica tipo,
		UUID idCanale,
		String message,
		Instant createdAt,
		Instant readAt
) {
	public static NotificaResponse da(Notifica n) {
		return new NotificaResponse(
				n.getId(),
				n.getTipo(),
				n.getCanale() == null ? null : n.getCanale().getId(),
				n.getMessage(),
				n.getCreatedAt(),
				n.getReadAt()
		);
	}
}
