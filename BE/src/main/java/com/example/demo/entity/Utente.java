package com.example.demo.entity;

import com.example.demo.enums.Ruolo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utente {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "username", nullable = false, unique = true, length = 50)
	private String username;

	/**
	 * La colonna e' dimensionata per contenere un hash BCrypt (60 caratteri).
	 * L'hashing va fatto nel service prima del save, non qui.
	 */
	@Column(name = "password", nullable = false, length = 100)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(name = "ruolo", nullable = false, length = 20)
	private Ruolo ruolo = Ruolo.USER;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
