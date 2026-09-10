package com.example.demo.service;

import com.example.demo.dto.NuovaNotificaRequest;
import com.example.demo.entity.Canale;
import com.example.demo.entity.Iscrizione;
import com.example.demo.entity.Utente;
import com.example.demo.enums.TipoNotifica;
import com.example.demo.repository.*;
import com.example.demo.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test end-to-end del push: server vero, client WebSocket vero.
 *
 * NON e' @Transactional di proposito. Il push parte in afterCommit, quindi con una
 * transazione di test annullata non scatterebbe mai: qui serve un commit reale,
 * e la pulizia dei dati la facciamo a mano.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificaPushTest {

	@LocalServerPort int porta;

	@Autowired NotificaService notificaService;
	@Autowired JwtService jwtService;
	@Autowired NotificaRepository notificaRepository;
	@Autowired UtenteRepository utenteRepository;
	@Autowired CanaleRepository canaleRepository;
	@Autowired IscrizioneRepository iscrizioneRepository;

	private Utente alice, bob;
	private Canale canale;

	@BeforeEach
	void setUp() {
		alice = nuovoUtente("push-alice");
		bob = nuovoUtente("push-bob");
		canale = new Canale();
		canale.setNome("push-canale");
		canale.setCreatore(alice);
		canale = canaleRepository.save(canale);
		iscrivi(alice, canale);
		iscrivi(bob, canale);
	}

	@AfterEach
	void pulisci() {
		notificaRepository.deleteAll(notificaRepository.findAll().stream()
				.filter(n -> n.getDestinatario().getId().equals(alice.getId())
						|| n.getDestinatario().getId().equals(bob.getId()))
				.toList());
		iscrizioneRepository.deleteAll(iscrizioneRepository.findAll().stream()
				.filter(i -> i.getCanale().getId().equals(canale.getId()))
				.toList());
		canaleRepository.delete(canale);
		utenteRepository.deleteAll(List.of(alice, bob));
	}

	@Test
	@DisplayName("Notifica di canale: arriva subito al client connesso su quel topic")
	void pushSuTopicDiCanale() throws Exception {
		Raccoglitore r = new Raccoglitore(1);
		try (WebSocketSession sessione = connetti(alice, canale.getId(), r)) {

			notificaService.crea(new NuovaNotificaRequest(
					TipoNotifica.CANALE, null, canale.getId(), "arrivo in tempo reale"));

			assertThat(r.latch.await(5, TimeUnit.SECONDS))
					.as("il messaggio deve arrivare entro 5s").isTrue();
			assertThat(r.messaggi).hasSize(1);
			assertThat(r.messaggi.getFirst())
					.contains("arrivo in tempo reale")
					.contains("\"tipo\":\"CANALE\"")
					.contains(canale.getId().toString());
		}
	}

	@Test
	@DisplayName("Notifica di canale: NON arriva a chi e' in ascolto su un altro topic")
	void nessunPushSuTopicDiverso() throws Exception {
		Raccoglitore r = new Raccoglitore(1);
		try (WebSocketSession sessione = connetti(alice, UUID.randomUUID(), r)) {

			notificaService.crea(new NuovaNotificaRequest(
					TipoNotifica.CANALE, null, canale.getId(), "non deve arrivare"));

			assertThat(r.latch.await(2, TimeUnit.SECONDS))
					.as("nessun messaggio atteso").isFalse();
			assertThat(r.messaggi).isEmpty();
		}
	}

	@Test
	@DisplayName("Sessione senza topic (la campanella): riceve anche le notifiche di canale")
	void sessioneGeneraleRiceveAncheIlCanale() throws Exception {
		Raccoglitore r = new Raccoglitore(1);
		try (WebSocketSession sessione = connetti(alice, null, r)) {

			notificaService.crea(new NuovaNotificaRequest(
					TipoNotifica.CANALE, null, canale.getId(), "anche alla campanella"));

			assertThat(r.latch.await(5, TimeUnit.SECONDS))
					.as("la vista generale deve ricevere tutto").isTrue();
			assertThat(r.messaggi.getFirst()).contains("anche alla campanella");
		}
	}

	@Test
	@DisplayName("Notifica PERSONAL: arriva anche senza topic, e solo al destinatario")
	void pushPersonalSoloAlDestinatario() throws Exception {
		Raccoglitore perAlice = new Raccoglitore(1);
		Raccoglitore perBob = new Raccoglitore(1);
		try (WebSocketSession sa = connetti(alice, null, perAlice);
		     WebSocketSession sb = connetti(bob, null, perBob)) {

			notificaService.crea(new NuovaNotificaRequest(
					TipoNotifica.PERSONAL, alice.getId(), null, "solo per alice"));

			assertThat(perAlice.latch.await(5, TimeUnit.SECONDS)).isTrue();
			assertThat(perAlice.messaggi.getFirst()).contains("solo per alice");

			assertThat(perBob.latch.await(2, TimeUnit.SECONDS))
					.as("bob non deve ricevere nulla").isFalse();
			assertThat(perBob.messaggi).isEmpty();
		}
	}

	@Test
	@DisplayName("Destinatario non connesso: la notifica resta comunque sul database")
	void nessunaSessioneAperta() {
		var create = notificaService.crea(new NuovaNotificaRequest(
				TipoNotifica.PERSONAL, bob.getId(), null, "la leggera' dopo"));

		assertThat(create).hasSize(1);
		assertThat(notificaRepository.findById(create.getFirst().id())).isPresent();
		assertThat(notificaService.contaNonLette(bob.getId())).isEqualTo(1);
	}

	// --- helper ---

	/** Il client si autentica col JWT, come fara' il frontend. */
	private WebSocketSession connetti(Utente utente, UUID topic, Raccoglitore r) throws Exception {
		String url = "ws://localhost:" + porta + "/ws/notifiche?token=" + jwtService.genera(utente)
				+ (topic == null ? "" : "&topic=" + topic);
		return new StandardWebSocketClient().execute(r, url).get(5, TimeUnit.SECONDS);
	}

	private static class Raccoglitore extends TextWebSocketHandler {
		final List<String> messaggi = new CopyOnWriteArrayList<>();
		final CountDownLatch latch;

		Raccoglitore(int attesi) {
			this.latch = new CountDownLatch(attesi);
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			messaggi.add(message.getPayload());
			latch.countDown();
		}
	}

	private Utente nuovoUtente(String prefisso) {
		Utente u = new Utente();
		u.setUsername(prefisso + "-" + UUID.randomUUID());
		u.setPassword("hash-finto");
		return utenteRepository.save(u);
	}

	private void iscrivi(Utente u, Canale c) {
		Iscrizione i = new Iscrizione();
		i.setUtente(u);
		i.setCanale(c);
		iscrizioneRepository.save(i);
	}
}
