package com.example.demo.controller;

import com.example.demo.service.IscrizioneService;
import com.example.demo.web.UtenteCorrente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/iscrizioni")
public class IscrizioneController {

	private final IscrizioneService iscrizioneService;

	public IscrizioneController(IscrizioneService iscrizioneService) {
		this.iscrizioneService = iscrizioneService;
	}

	/** FollowCanale: l'utente corrente inizia a seguire il canale. */
	@PostMapping("/{canaleId}")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, UUID> follow(@UtenteCorrente UUID utenteId, @PathVariable UUID canaleId) {
		return Map.of("idIscrizione", iscrizioneService.follow(utenteId, canaleId));
	}

	/** UnfollowCanale: 204, nessun corpo da restituire. */
	@DeleteMapping("/{canaleId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unfollow(@UtenteCorrente UUID utenteId, @PathVariable UUID canaleId) {
		iscrizioneService.unfollow(utenteId, canaleId);
	}

	/** Comodo per il frontend: sapere se mostrare "Segui" o "Smetti di seguire". */
	@GetMapping("/{canaleId}")
	public Map<String, Boolean> statoIscrizione(@UtenteCorrente UUID utenteId,
	                                            @PathVariable UUID canaleId) {
		return Map.of("iscritto", iscrizioneService.eIscritto(utenteId, canaleId));
	}
}
