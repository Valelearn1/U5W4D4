package com.example.demo.service;

import com.example.demo.dto.CanaleResponse;
import com.example.demo.dto.NuovaNotificaRequest;
import com.example.demo.dto.NuovoCanaleRequest;
import com.example.demo.entity.Utente;
import com.example.demo.enums.TipoNotifica;
import com.example.demo.repository.UtenteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressione: le risposte di creazione devono gia' contenere createdAt.
 * Con un semplice save() @CreationTimestamp non e' ancora valorizzato e il campo
 * usciva null - anche nel payload inviato sul WebSocket.
 */
@SpringBootTest
@Transactional
class RegressioneCreatedAtTest {

	@Autowired CanaleService canaleService;
	@Autowired NotificaService notificaService;
	@Autowired UtenteRepository utenteRepository;

	@Test
	@DisplayName("POST canale: createdAt valorizzato nella risposta")
	void canaleCreatedAt() {
		Utente u = nuovoUtente();
		CanaleResponse c = canaleService.crea(new NuovoCanaleRequest("nuovo", "x", u.getId()));
		assertThat(c.createdAt()).isNotNull();
	}

	@Test
	@DisplayName("POST notifica: createdAt valorizzato nella risposta")
	void notificaCreatedAt() {
		Utente u = nuovoUtente();
		var create = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.PERSONAL, u.getId(), null, "ciao"));
		assertThat(create).hasSize(1);
		assertThat(create.getFirst().createdAt()).isNotNull();
	}

	private Utente nuovoUtente() {
		Utente u = new Utente();
		u.setUsername("createdat-" + UUID.randomUUID());
		u.setPassword("hash-finto");
		return utenteRepository.save(u);
	}
}
