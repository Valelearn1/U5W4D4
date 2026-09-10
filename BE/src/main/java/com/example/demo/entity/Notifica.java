package com.example.demo.entity;

import com.example.demo.enums.TipoNotifica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "notifica",
		indexes = {
				// supporta la GetAll: notifiche di un utente, non lette prima, poi per data DESC
				@Index(name = "idx_notifica_destinatario_read", columnList = "id_destinatario, read_at, created_at")
		}
)
// Se il canale non e' valorizzato, il tipo deve per forza essere PERSONAL o ALL.
@Check(
		name = "ck_notifica_canale_tipo",
		constraints = "id_canale IS NOT NULL OR tipo IN ('PERSONAL', 'ALL')"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notifica {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	/** Destinatario della notifica: un utente puo' averne molte (1:N). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_destinatario", nullable = false)
	private Utente destinatario;

	/** Se valorizzato la notifica proviene da un canale, altrimenti e' di sistema (0:N). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_canale")
	private Canale canale;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoNotifica tipo;

	@Column(name = "message", columnDefinition = "TEXT", nullable = false)
	private String message;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** NULL finche' la notifica non viene letta. */
	@Column(name = "read_at")
	private Instant readAt;
}
