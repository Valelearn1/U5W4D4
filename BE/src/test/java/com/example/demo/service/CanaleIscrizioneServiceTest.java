package com.example.demo.service;

import com.example.demo.dto.CanaleResponse;
import com.example.demo.dto.NuovoCanaleRequest;
import com.example.demo.entity.Canale;
import com.example.demo.entity.Utente;
import com.example.demo.repository.CanaleRepository;
import com.example.demo.repository.IscrizioneRepository;
import com.example.demo.repository.UtenteRepository;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CanaleIscrizioneServiceTest {

	@Autowired CanaleService canaleService;
	@Autowired IscrizioneService iscrizioneService;
	@Autowired CanaleRepository canaleRepository;
	@Autowired UtenteRepository utenteRepository;
	@Autowired IscrizioneRepository iscrizioneRepository;
	@Autowired EntityManager em;

	private Utente alice, bob;

	@BeforeEach
	void setUp() {
		alice = nuovoUtente("alice");
		bob = nuovoUtente("bob");
	}

	// ---------- CanaleService ----------

	@Test
	@DisplayName("GET ALL CANALI: dal piu' recente al piu' vecchio")
	void tuttiICanaliOrdinatiDesc() {
		creaCanale("vecchio", 30);
		creaCanale("medio", 20);
		creaCanale("nuovo", 5);
		em.flush();
		em.clear();

		Page<CanaleResponse> pagina = canaleService.getTutti(0, CanaleService.DIMENSIONE_PAGINA);

		assertThat(pagina.getContent())
				.extracting(CanaleResponse::nome)
				.containsExactly("nuovo", "medio", "vecchio");
	}

	@Test
	@DisplayName("GET ALL CANALI: LIMIT 15 per pagina")
	void tuttiICanaliPaginati() {
		for (int i = 0; i < 18; i++) {
			creaCanale("c" + i, 18 - i);
		}
		em.flush();
		em.clear();

		assertThat(canaleService.getTutti(0, 15).getContent()).hasSize(15);
		assertThat(canaleService.getTutti(1, 15).getContent()).hasSize(3);
		assertThat(canaleService.getTutti(0, 15).getTotalElements()).isEqualTo(18);
	}

	@Test
	@DisplayName("GET Canali Iscritto: solo quelli seguiti, dal piu' recente")
	void canaliIscritto() {
		Canale seguitoVecchio = creaCanale("seguito-vecchio", 30);
		Canale seguitoNuovo = creaCanale("seguito-nuovo", 10);
		Canale nonSeguito = creaCanale("non-seguito", 20);
		em.flush();

		iscrizioneService.follow(alice.getId(), seguitoVecchio.getId());
		iscrizioneService.follow(alice.getId(), seguitoNuovo.getId());
		em.flush();
		em.clear();

		Page<CanaleResponse> pagina = canaleService.getIscrittoDa(alice.getId(), 0, 15);

		assertThat(pagina.getContent())
				.extracting(CanaleResponse::nome)
				.containsExactly("seguito-nuovo", "seguito-vecchio");
		assertThat(pagina.getContent())
				.extracting(CanaleResponse::nome)
				.doesNotContain("non-seguito");
	}

	@Test
	@DisplayName("GET Canali Iscritto: le iscrizioni di un utente non si vedono da un altro")
	void canaliIscrittoIsolatiPerUtente() {
		Canale c = creaCanale("solo-di-alice", 5);
		em.flush();
		iscrizioneService.follow(alice.getId(), c.getId());
		em.flush();
		em.clear();

		assertThat(canaleService.getIscrittoDa(alice.getId(), 0, 15).getContent()).hasSize(1);
		assertThat(canaleService.getIscrittoDa(bob.getId(), 0, 15).getContent()).isEmpty();
	}

	@Test
	@DisplayName("Creazione canale: valida nome e creatore")
	void creaCanaleValidazione() {
		CanaleResponse creato = canaleService.crea(
				new NuovoCanaleRequest("  generale  ", "descrizione", alice.getId()));
		assertThat(creato.nome()).isEqualTo("generale");     // trim applicato
		assertThat(creato.id()).isNotNull();

		assertThatThrownBy(() -> canaleService.crea(new NuovoCanaleRequest("  ", "x", alice.getId())))
				.hasMessageContaining("nome");

		assertThatThrownBy(() -> canaleService.crea(new NuovoCanaleRequest("ok", "x", null)))
				.hasMessageContaining("idCreatore");

		assertThatThrownBy(() -> canaleService.crea(new NuovoCanaleRequest("ok", "x", UUID.randomUUID())))
				.isInstanceOf(NoSuchElementException.class);
	}

	// ---------- IscrizioneService ----------

	@Test
	@DisplayName("Follow: crea l'iscrizione")
	void follow() {
		Canale c = creaCanale("generale", 5);
		em.flush();

		UUID idIscrizione = iscrizioneService.follow(alice.getId(), c.getId());

		assertThat(idIscrizione).isNotNull();
		assertThat(iscrizioneService.eIscritto(alice.getId(), c.getId())).isTrue();
		assertThat(iscrizioneService.contaIscritti(c.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("Follow due volte: la seconda viene rifiutata")
	void followDuplicato() {
		Canale c = creaCanale("generale", 5);
		em.flush();
		iscrizioneService.follow(alice.getId(), c.getId());

		assertThatThrownBy(() -> iscrizioneService.follow(alice.getId(), c.getId()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("gia'");

		assertThat(iscrizioneService.contaIscritti(c.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("Follow: utente o canale inesistenti")
	void followEntitaInesistenti() {
		Canale c = creaCanale("generale", 5);
		em.flush();

		assertThatThrownBy(() -> iscrizioneService.follow(UUID.randomUUID(), c.getId()))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("utente");

		assertThatThrownBy(() -> iscrizioneService.follow(alice.getId(), UUID.randomUUID()))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("canale");
	}

	@Test
	@DisplayName("Unfollow: rimuove l'iscrizione e non tocca gli altri iscritti")
	void unfollow() {
		Canale c = creaCanale("generale", 5);
		em.flush();
		iscrizioneService.follow(alice.getId(), c.getId());
		iscrizioneService.follow(bob.getId(), c.getId());
		assertThat(iscrizioneService.contaIscritti(c.getId())).isEqualTo(2);

		iscrizioneService.unfollow(alice.getId(), c.getId());

		assertThat(iscrizioneService.eIscritto(alice.getId(), c.getId())).isFalse();
		assertThat(iscrizioneService.eIscritto(bob.getId(), c.getId())).isTrue();
		assertThat(iscrizioneService.contaIscritti(c.getId())).isEqualTo(1);
	}

	@Test
	@DisplayName("Unfollow senza essere iscritto: errore")
	void unfollowNonIscritto() {
		Canale c = creaCanale("generale", 5);
		em.flush();

		assertThatThrownBy(() -> iscrizioneService.unfollow(alice.getId(), c.getId()))
				.isInstanceOf(NoSuchElementException.class);
	}

	@Test
	@DisplayName("Follow -> unfollow -> follow di nuovo: consentito")
	void riFollowDopoUnfollow() {
		Canale c = creaCanale("generale", 5);
		em.flush();

		iscrizioneService.follow(alice.getId(), c.getId());
		iscrizioneService.unfollow(alice.getId(), c.getId());
		em.flush();

		assertThat(iscrizioneService.follow(alice.getId(), c.getId())).isNotNull();
		assertThat(iscrizioneService.contaIscritti(c.getId())).isEqualTo(1);
	}

	// --- helper ---

	private Utente nuovoUtente(String prefisso) {
		Utente u = new Utente();
		u.setUsername(prefisso + "-" + UUID.randomUUID());
		u.setPassword("hash-finto");
		return utenteRepository.save(u);
	}

	/** Crea un canale e ne forza createdAt a "minuti fa", per testare l'ordinamento. */
	private Canale creaCanale(String nome, int minutiFa) {
		Canale c = new Canale();
		c.setNome(nome);
		c.setDescrizione("descrizione di " + nome);
		c.setCreatore(alice);
		c = canaleRepository.saveAndFlush(c);
		em.createNativeQuery("UPDATE canale SET created_at = ?1 WHERE id = ?2")
				.setParameter(1, Instant.now().minus(minutiFa, ChronoUnit.MINUTES))
				.setParameter(2, c.getId())
				.executeUpdate();
		return c;
	}
}
