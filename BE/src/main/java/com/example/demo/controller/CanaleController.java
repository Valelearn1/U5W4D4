package com.example.demo.controller;

import com.example.demo.dto.CanaleResponse;
import com.example.demo.dto.NuovoCanaleRequest;
import com.example.demo.dto.PaginaResponse;
import com.example.demo.service.CanaleService;
import com.example.demo.web.UtenteCorrente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/canali")
public class CanaleController {

	private final CanaleService canaleService;

	public CanaleController(CanaleService canaleService) {
		this.canaleService = canaleService;
	}

	/** GET ALL CANALI: tutti, dal piu' recente. */
	@GetMapping
	public PaginaResponse<CanaleResponse> tutti(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + CanaleService.DIMENSIONE_PAGINA) int size) {
		return PaginaResponse.da(canaleService.getTutti(page, size));
	}

	/** GET Canali Iscritto: solo quelli seguiti dall'utente corrente. */
	@GetMapping("/iscritto")
	public PaginaResponse<CanaleResponse> iscritto(
			@UtenteCorrente UUID utenteId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + CanaleService.DIMENSIONE_PAGINA) int size) {
		return PaginaResponse.da(canaleService.getIscrittoDa(utenteId, page, size));
	}

	@GetMapping("/{canaleId}")
	public CanaleResponse perId(@PathVariable UUID canaleId) {
		return canaleService.getPerId(canaleId);
	}

	/**
	 * Creazione. Il creatore e' SEMPRE l'utente corrente: un eventuale idCreatore
	 * inviato nel corpo viene ignorato, altrimenti si potrebbero creare canali a nome di altri.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CanaleResponse crea(@UtenteCorrente UUID utenteId,
	                           @RequestBody NuovoCanaleRequest corpo) {
		return canaleService.crea(
				new NuovoCanaleRequest(corpo.nome(), corpo.descrizione(), utenteId));
	}
}
