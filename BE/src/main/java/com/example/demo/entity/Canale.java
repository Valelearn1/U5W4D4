package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "canale")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Canale {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "nome", nullable = false, length = 100)
	private String nome;

	@Column(name = "descrizione", columnDefinition = "TEXT")
	private String descrizione;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** Utente che ha creato il canale: un utente puo' avere molti canali (1:N). */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_utente", nullable = false)
	private Utente creatore;
}
