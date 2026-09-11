package com.example.demo.service;

import com.example.demo.dto.CanaleResponse;
import com.example.demo.dto.NuovoCanaleRequest;
import com.example.demo.entity.Canale;
import com.example.demo.entity.Utente;
import com.example.demo.repository.CanaleRepository;
import com.example.demo.repository.UtenteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
public class CanaleService {

	/** LIMIT 15 della specifica. */
	public static final int DIMENSIONE_PAGINA = 15;

	private final CanaleRepository canaleRepository;
	private final UtenteRepository utenteRepository;

	public CanaleService(CanaleRepository canaleRepository, UtenteRepository utenteRepository) {
		this.canaleRepository = canaleRepository;
		this.utenteRepository = utenteRepository;
	}

	/** GET ALL CANALI: tutti, dal piu' recente, paginati. */
	@Transactional(readOnly = true)
	public Page<CanaleResponse> getTutti(int page, int size) {
		return canaleRepository
				.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
				.map(CanaleResponse::da);
	}

	/** GET Canali Iscritto: solo quelli seguiti dall'utente, dal piu' recente. */
	@Transactional(readOnly = true)
	public Page<CanaleResponse> getIscrittoDa(UUID utenteId, int page, int size) {
		return canaleRepository
				.findIscrittoDa(utenteId, PageRequest.of(page, size))
				.map(CanaleResponse::da);
	}

	@Transactional(readOnly = true)
	public CanaleResponse getPerId(UUID canaleId) {
		return canaleRepository.findById(canaleId)
				.map(CanaleResponse::da)
				.orElseThrow(() -> new NoSuchElementException("canale non trovato: " + canaleId));
	}

	/**
	 * Creazione di un canale. Non e' fra gli endpoint della specifica, ma senza
	 * non ci sarebbe modo di popolare i canali da seguire.
	 */
	@Transactional
	public CanaleResponse crea(NuovoCanaleRequest richiesta) {
		if (richiesta.nome() == null || richiesta.nome().isBlank()) {
			throw new IllegalArgumentException("nome obbligatorio");
		}
		if (richiesta.idCreatore() == null) {
			throw new IllegalArgumentException("idCreatore obbligatorio");
		}
		Utente creatore = utenteRepository.findById(richiesta.idCreatore())
				.orElseThrow(() -> new NoSuchElementException("utente non trovato: " + richiesta.idCreatore()));

		Canale canale = new Canale();
		canale.setNome(richiesta.nome().trim());
		canale.setDescrizione(richiesta.descrizione());
		canale.setCreatore(creatore);

		// saveAndFlush: serve a far valorizzare @CreationTimestamp prima di leggere createdAt
		Canale salvato = canaleRepository.saveAndFlush(canale);
		log.info("canale creato: {} ({}) da {}", salvato.getNome(), salvato.getId(), creatore.getUsername());
		return CanaleResponse.da(salvato);
	}
}
