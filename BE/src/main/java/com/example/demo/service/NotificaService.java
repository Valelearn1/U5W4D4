package com.example.demo.service;

import com.example.demo.dto.NotificaResponse;
import com.example.demo.dto.NuovaNotificaRequest;
import com.example.demo.entity.Canale;
import com.example.demo.entity.Notifica;
import com.example.demo.enums.TipoNotifica;
import com.example.demo.repository.CanaleRepository;
import com.example.demo.repository.IscrizioneRepository;
import com.example.demo.repository.NotificaRepository;
import com.example.demo.repository.UtenteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
public class NotificaService {

	private final NotificaRepository notificaRepository;
	private final IscrizioneRepository iscrizioneRepository;
	private final UtenteRepository utenteRepository;
	private final CanaleRepository canaleRepository;
	private final NotificaPushService pushService;

	public NotificaService(NotificaRepository notificaRepository,
	                       IscrizioneRepository iscrizioneRepository,
	                       UtenteRepository utenteRepository,
	                       CanaleRepository canaleRepository,
	                       NotificaPushService pushService) {
		this.notificaRepository = notificaRepository;
		this.iscrizioneRepository = iscrizioneRepository;
		this.utenteRepository = utenteRepository;
		this.canaleRepository = canaleRepository;
		this.pushService = pushService;
	}

	/** GetAll: prima le non lette, poi le lette, entrambe dalla piu' recente. */
	@Transactional(readOnly = true)
	public Page<NotificaResponse> getFeed(UUID utenteId, int page, int size) {
		return notificaRepository
				.findFeedByDestinatario(utenteId, PageRequest.of(page, size))
				.map(NotificaResponse::da);
	}

	/** CountNotification: quante non lette ha l'utente. */
	@Transactional(readOnly = true)
	public long contaNonLette(UUID utenteId) {
		return notificaRepository.countByDestinatarioIdAndReadAtIsNull(utenteId);
	}

	/**
	 * ReadNotification: valorizza read_at di UNA notifica.
	 * Richiede anche l'utente: senza questo controllo chiunque conoscesse un UUID
	 * potrebbe segnare come lette le notifiche di un altro.
	 */
	@Transactional
	public NotificaResponse marcaComeLetta(UUID notificaId, UUID utenteId) {
		Notifica notifica = notificaRepository
				.findByIdAndDestinatarioId(notificaId, utenteId)
				.orElseThrow(() -> new NoSuchElementException("notifica non trovata: " + notificaId));

		if (notifica.getReadAt() == null) {
			notifica.setReadAt(Instant.now());
		}
		return NotificaResponse.da(notifica);
	}

	/** ReadAllNotification: una sola UPDATE per tutte le non lette. Ritorna quante ne ha toccate. */
	@Transactional
	public int marcaTutteComeLette(UUID utenteId) {
		return notificaRepository.marcaTutteComeLette(utenteId, Instant.now());
	}

	/**
	 * Creazione. Il numero di righe scritte dipende dal tipo:
	 *   PERSONAL -> 1, per il destinatario indicato
	 *   CANALE   -> una per ogni iscritto al canale
	 *   ALL      -> una per ogni utente registrato
	 */
	@Transactional
	public List<NotificaResponse> crea(NuovaNotificaRequest richiesta) {
		validare(richiesta);

		Canale canale = null;
		List<UUID> destinatari;

		switch (richiesta.tipo()) {
			case PERSONAL -> {
				if (!utenteRepository.existsById(richiesta.idDestinatario())) {
					throw new NoSuchElementException("utente non trovato: " + richiesta.idDestinatario());
				}
				destinatari = List.of(richiesta.idDestinatario());
			}
			case CANALE -> {
				canale = canaleRepository.findById(richiesta.idCanale())
						.orElseThrow(() -> new NoSuchElementException("canale non trovato: " + richiesta.idCanale()));
				destinatari = iscrizioneRepository.findUtenteIdsByCanaleId(canale.getId());
			}
			case ALL -> destinatari = utenteRepository.findAllIds();
			default -> throw new IllegalArgumentException("tipo non gestito: " + richiesta.tipo());
		}

		if (destinatari.isEmpty()) {
			return List.of();
		}

		final Canale canaleFinale = canale;
		List<Notifica> daSalvare = destinatari.stream()
				.map(destinatarioId -> {
					Notifica n = new Notifica();
					// getReferenceById restituisce un proxy: fissa la FK senza una SELECT per ogni destinatario
					n.setDestinatario(utenteRepository.getReferenceById(destinatarioId));
					n.setCanale(canaleFinale);
					n.setTipo(richiesta.tipo());
					n.setMessage(richiesta.message());
					return n;
				})
				.toList();

		// saveAllAndFlush, non saveAll: senza il flush @CreationTimestamp non e' ancora
		// stato valorizzato e createdAt uscirebbe null nella risposta e nel push WebSocket.
		List<Notifica> salvate = notificaRepository.saveAllAndFlush(daSalvare);
		log.info("create {} notifiche di tipo {}{}", salvate.size(), richiesta.tipo(),
				canaleFinale == null ? "" : " sul canale " + canaleFinale.getId());
		inviaDopoIlCommit(salvate);

		return salvate.stream().map(NotificaResponse::da).toList();
	}

	/**
	 * Il push parte solo a transazione confermata. Se lo facessimo subito e poi la
	 * transazione andasse in rollback, il client avrebbe gia' ricevuto una notifica
	 * che sul database non esiste.
	 */
	private void inviaDopoIlCommit(List<Notifica> notifiche) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			notifiche.forEach(pushService::push);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				notifiche.forEach(pushService::push);
			}
		});
	}

	private static void validare(NuovaNotificaRequest r) {
		if (r.tipo() == null) {
			throw new IllegalArgumentException("tipo obbligatorio");
		}
		if (r.message() == null || r.message().isBlank()) {
			throw new IllegalArgumentException("message obbligatorio");
		}
		if (r.tipo() == TipoNotifica.PERSONAL && r.idDestinatario() == null) {
			throw new IllegalArgumentException("tipo PERSONAL richiede idDestinatario");
		}
		if (r.tipo() == TipoNotifica.CANALE && r.idCanale() == null) {
			throw new IllegalArgumentException("tipo CANALE richiede idCanale");
		}
		if (r.tipo() != TipoNotifica.CANALE && r.idCanale() != null) {
			throw new IllegalArgumentException("idCanale ammesso solo con tipo CANALE");
		}
	}
}
