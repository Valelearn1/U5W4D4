package com.example.demo.controller;

import com.example.demo.dto.NotificaResponse;
import com.example.demo.dto.NuovaNotificaRequest;
import com.example.demo.dto.PaginaResponse;
import com.example.demo.service.CanaleService;
import com.example.demo.service.NotificaService;
import com.example.demo.web.UtenteCorrente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifiche")
public class NotificaController {

	private final NotificaService notificaService;

	public NotificaController(NotificaService notificaService) {
		this.notificaService = notificaService;
	}

	/** GetAll: prima le non lette, poi le lette, entrambe dalla piu' recente. */
	@GetMapping
	public PaginaResponse<NotificaResponse> tutte(
			@UtenteCorrente UUID utenteId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + CanaleService.DIMENSIONE_PAGINA) int size) {
		return PaginaResponse.da(notificaService.getFeed(utenteId, page, size));
	}

	/** CountNotification: quante non lette ha l'utente corrente. */
	@GetMapping("/non-lette/conteggio")
	public Map<String, Long> conteggioNonLette(@UtenteCorrente UUID utenteId) {
		return Map.of("nonLette", notificaService.contaNonLette(utenteId));
	}

	/** ReadNotification: valorizza read_at di UNA notifica dell'utente corrente. */
	@PatchMapping("/{notificaId}/letta")
	public NotificaResponse marcaComeLetta(@UtenteCorrente UUID utenteId,
	                                       @PathVariable UUID notificaId) {
		return notificaService.marcaComeLetta(notificaId, utenteId);
	}

	/** ReadAllNotification: valorizza read_at di tutte le non lette dell'utente corrente. */
	@PostMapping("/lette")
	public Map<String, Integer> marcaTutteComeLette(@UtenteCorrente UUID utenteId) {
		return Map.of("aggiornate", notificaService.marcaTutteComeLette(utenteId));
	}

	/**
	 * Creazione di una notifica. Restituisce tutte le righe generate:
	 * una per PERSONAL, una per iscritto con CANALE, una per utente con ALL.
	 *
	 * DA RIVEDERE con la security: oggi chiunque puo' inviare notifiche a chiunque.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public List<NotificaResponse> crea(@RequestBody NuovaNotificaRequest corpo) {
		return notificaService.crea(corpo);
	}
}
