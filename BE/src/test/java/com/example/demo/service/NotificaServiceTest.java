package com.example.demo.service;

import com.example.demo.dto.NotificaResponse;
import com.example.demo.dto.NuovaNotificaRequest;
import com.example.demo.entity.Canale;
import com.example.demo.entity.Iscrizione;
import com.example.demo.entity.Notifica;
import com.example.demo.entity.Utente;
import com.example.demo.enums.TipoNotifica;
import com.example.demo.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional // ogni test gira in transazione e viene annullato: il DB resta pulito
class NotificaServiceTest {

	@Autowired NotificaService notificaService;
	@Autowired NotificaRepository notificaRepository;
	@Autowired UtenteRepository utenteRepository;
	@Autowired CanaleRepository canaleRepository;
	@Autowired IscrizioneRepository iscrizioneRepository;
	@Autowired EntityManager em;

	private Utente alice, bob, carol;
	private Canale canale;

	@BeforeEach
	void setUp() {
		alice = nuovoUtente("alice");
		bob = nuovoUtente("bob");
		carol = nuovoUtente("carol");   // NON iscritta al canale

		canale = new Canale();
		canale.setNome("generale");
		canale.setDescrizione("canale di prova");
		canale.setCreatore(alice);
		canale = canaleRepository.save(canale);

		iscrivi(alice, canale);
		iscrivi(bob, canale);
	}

	@Test
	@DisplayName("PERSONAL: crea una sola notifica, per il destinatario indicato")
	void creaPersonal() {
		List<NotificaResponse> create = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.PERSONAL, bob.getId(), null, "solo per te"));

		assertThat(create).hasSize(1);
		assertThat(create.getFirst().idCanale()).isNull();
		assertThat(create.getFirst().tipo()).isEqualTo(TipoNotifica.PERSONAL);
		assertThat(notificaService.contaNonLette(bob.getId())).isEqualTo(1);
		assertThat(notificaService.contaNonLette(alice.getId())).isZero();
	}

	@Test
	@DisplayName("CANALE: una notifica per ogni iscritto, nessuna per i non iscritti")
	void creaCanaleFanOut() {
		List<NotificaResponse> create = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.CANALE, null, canale.getId(), "novita' sul canale"));

		assertThat(create).hasSize(2);                                  // alice + bob
		assertThat(create).allSatisfy(n -> assertThat(n.idCanale()).isEqualTo(canale.getId()));
		assertThat(notificaService.contaNonLette(alice.getId())).isEqualTo(1);
		assertThat(notificaService.contaNonLette(bob.getId())).isEqualTo(1);
		assertThat(notificaService.contaNonLette(carol.getId())).isZero();  // non iscritta
	}

	@Test
	@DisplayName("ALL: una notifica per ogni utente registrato")
	void creaAll() {
		long utentiTotali = utenteRepository.count();

		List<NotificaResponse> create = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.ALL, null, null, "manutenzione stanotte"));

		assertThat(create).hasSize((int) utentiTotali);
		assertThat(notificaService.contaNonLette(carol.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("Feed: prima le non lette, poi le lette; dentro ogni gruppo dalla piu' recente")
	void ordinamentoFeed() {
		Notifica vecchiaLetta = salvaNotifica(alice, "vecchia letta", 30);
		Notifica recenteLetta = salvaNotifica(alice, "recente letta", 20);
		Notifica vecchiaNonLetta = salvaNotifica(alice, "vecchia non letta", 10);
		Notifica recenteNonLetta = salvaNotifica(alice, "recente non letta", 1);

		segnaLetta(vecchiaLetta);
		segnaLetta(recenteLetta);
		em.flush();
		em.clear();

		Page<NotificaResponse> feed = notificaService.getFeed(alice.getId(), 0, 15);

		assertThat(feed.getContent())
				.extracting(NotificaResponse::message)
				.containsExactly(
						"recente non letta",   // non lette, dalla piu' recente
						"vecchia non letta",
						"recente letta",       // poi le lette, dalla piu' recente
						"vecchia letta");
	}

	@Test
	@DisplayName("Feed paginato: LIMIT 15 per pagina")
	void feedPaginato() {
		for (int i = 0; i < 20; i++) {
			salvaNotifica(alice, "n" + i, 20 - i);
		}
		em.flush();
		em.clear();

		Page<NotificaResponse> pagina0 = notificaService.getFeed(alice.getId(), 0, 15);
		assertThat(pagina0.getContent()).hasSize(15);
		assertThat(pagina0.getTotalElements()).isEqualTo(20);
		assertThat(notificaService.getFeed(alice.getId(), 1, 15).getContent()).hasSize(5);
	}

	@Test
	@DisplayName("ReadNotification: valorizza read_at una sola volta")
	void marcaComeLetta() {
		UUID id = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.PERSONAL, alice.getId(), null, "ciao")).getFirst().id();

		NotificaResponse letta = notificaService.marcaComeLetta(id, alice.getId());
		assertThat(letta.readAt()).isNotNull();

		Instant primaLettura = letta.readAt();
		NotificaResponse riletta = notificaService.marcaComeLetta(id, alice.getId());
		assertThat(riletta.readAt()).isEqualTo(primaLettura);   // non si sovrascrive
	}

	@Test
	@DisplayName("ReadNotification: non posso leggere le notifiche di un altro utente")
	void marcaComeLettaAltrui() {
		UUID id = notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.PERSONAL, alice.getId(), null, "riservata")).getFirst().id();

		assertThatThrownBy(() -> notificaService.marcaComeLetta(id, bob.getId()))
				.isInstanceOf(NoSuchElementException.class);

		assertThat(notificaService.contaNonLette(alice.getId())).isEqualTo(1);   // rimasta non letta
	}

	@Test
	@DisplayName("ReadAllNotification: azzera le non lette del solo utente indicato")
	void marcaTutteComeLette() {
		notificaService.crea(new NuovaNotificaRequest(TipoNotifica.CANALE, null, canale.getId(), "uno"));
		notificaService.crea(new NuovaNotificaRequest(TipoNotifica.CANALE, null, canale.getId(), "due"));
		assertThat(notificaService.contaNonLette(alice.getId())).isEqualTo(2);
		assertThat(notificaService.contaNonLette(bob.getId())).isEqualTo(2);

		int toccate = notificaService.marcaTutteComeLette(alice.getId());

		assertThat(toccate).isEqualTo(2);
		assertThat(notificaService.contaNonLette(alice.getId())).isZero();
		assertThat(notificaService.contaNonLette(bob.getId())).isEqualTo(2);   // bob non e' stato toccato
	}

	@Test
	@DisplayName("Validazione: combinazioni tipo/parametri non ammesse")
	void validazione() {
		assertThatThrownBy(() -> notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.PERSONAL, null, null, "x")))
				.hasMessageContaining("idDestinatario");

		assertThatThrownBy(() -> notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.CANALE, null, null, "x")))
				.hasMessageContaining("idCanale");

		assertThatThrownBy(() -> notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.ALL, null, canale.getId(), "x")))
				.hasMessageContaining("solo con tipo CANALE");

		assertThatThrownBy(() -> notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.ALL, null, null, "   ")))
				.hasMessageContaining("message");
	}

	@Test
	@DisplayName("Canale senza iscritti: nessuna notifica creata")
	void canaleSenzaIscritti() {
		Canale deserto = new Canale();
		deserto.setNome("deserto");
		deserto.setCreatore(carol);
		deserto = canaleRepository.save(deserto);

		assertThat(notificaService.crea(
				new NuovaNotificaRequest(TipoNotifica.CANALE, null, deserto.getId(), "c'e' nessuno?")))
				.isEmpty();
	}

	// --- helper ---

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

	/** Salva una notifica e le forza createdAt a "minuti fa", per poter testare l'ordinamento. */
	private Notifica salvaNotifica(Utente destinatario, String testo, int minutiFa) {
		Notifica n = new Notifica();
		n.setDestinatario(destinatario);
		n.setTipo(TipoNotifica.PERSONAL);
		n.setMessage(testo);
		n = notificaRepository.saveAndFlush(n);
		em.createNativeQuery("UPDATE notifica SET created_at = ?1 WHERE id = ?2")
				.setParameter(1, Instant.now().minus(minutiFa, ChronoUnit.MINUTES))
				.setParameter(2, n.getId())
				.executeUpdate();
		return n;
	}

	private void segnaLetta(Notifica n) {
		em.createNativeQuery("UPDATE notifica SET read_at = ?1 WHERE id = ?2")
				.setParameter(1, Instant.now())
				.setParameter(2, n.getId())
				.executeUpdate();
	}
}
