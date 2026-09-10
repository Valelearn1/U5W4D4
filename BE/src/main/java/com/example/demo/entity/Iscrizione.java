package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Tabella ponte fra Utente e Canale: rappresenta il "follow".
 * Il vincolo di unicita' impedisce che lo stesso utente segua due volte lo stesso canale.
 */
@Entity
@Table(
		name = "iscrizione",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_iscrizione_utente_canale",
				columnNames = {"id_utente", "id_canale"}
		)
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Iscrizione {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_utente", nullable = false)
	private Utente utente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_canale", nullable = false)
	private Canale canale;
}
