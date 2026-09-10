package com.example.demo.service;

import com.example.demo.entity.Canale;
import com.example.demo.entity.Iscrizione;
import com.example.demo.entity.Utente;
import com.example.demo.repository.CanaleRepository;
import com.example.demo.repository.IscrizioneRepository;
import com.example.demo.repository.UtenteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class IscrizioneService {

	private final IscrizioneRepository iscrizioneRepository;
	private final CanaleRepository canaleRepository;
	private final UtenteRepository utenteRepository;

	public IscrizioneService(IscrizioneRepository iscrizioneRepository,
	                         CanaleRepository canaleRepository,
	                         UtenteRepository utenteRepository) {
		this.iscrizioneRepository = iscrizioneRepository;
		this.canaleRepository = canaleRepository;
		this.utenteRepository = utenteRepository;
	}

	/**
	 * FollowCanale. Il controllo con existsBy serve a dare un errore chiaro,
	 * ma la garanzia vera e' il vincolo UNIQUE sul database: fra il controllo e
	 * la INSERT c'e' una finestra in cui due richieste contemporanee passerebbero
	 * entrambe, e li' e' il DB a fermare la seconda.
	 */
	@Transactional
	public UUID follow(UUID utenteId, UUID canaleId) {
		Utente utente = utenteRepository.findById(utenteId)
				.orElseThrow(() -> new NoSuchElementException("utente non trovato: " + utenteId));
		Canale canale = canaleRepository.findById(canaleId)
				.orElseThrow(() -> new NoSuchElementException("canale non trovato: " + canaleId));

		if (iscrizioneRepository.existsByUtenteIdAndCanaleId(utenteId, canaleId)) {
			throw new IllegalStateException("iscrizione gia' esistente");
		}

		Iscrizione iscrizione = new Iscrizione();
		iscrizione.setUtente(utente);
		iscrizione.setCanale(canale);

		try {
			return iscrizioneRepository.save(iscrizione).getId();
		} catch (DataIntegrityViolationException e) {
			throw new IllegalStateException("iscrizione gia' esistente", e);
		}
	}

	/** UnfollowCanale. */
	@Transactional
	public void unfollow(UUID utenteId, UUID canaleId) {
		Iscrizione iscrizione = iscrizioneRepository
				.findByUtenteIdAndCanaleId(utenteId, canaleId)
				.orElseThrow(() -> new NoSuchElementException("iscrizione non trovata"));
		iscrizioneRepository.delete(iscrizione);
	}

	@Transactional(readOnly = true)
	public boolean eIscritto(UUID utenteId, UUID canaleId) {
		return iscrizioneRepository.existsByUtenteIdAndCanaleId(utenteId, canaleId);
	}

	@Transactional(readOnly = true)
	public long contaIscritti(UUID canaleId) {
		return iscrizioneRepository.countByCanaleId(canaleId);
	}
}
