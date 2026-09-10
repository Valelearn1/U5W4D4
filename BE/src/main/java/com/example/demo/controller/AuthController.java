package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.security.UtenteAutenticato;
import com.example.demo.service.AuthService;
import com.example.demo.web.UtenteCorrente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/registrazione")
	@ResponseStatus(HttpStatus.CREATED)
	public UtenteResponse registra(@RequestBody RegistrazioneRequest corpo) {
		return authService.registra(corpo);
	}

	@PostMapping("/login")
	public LoginResponse login(@RequestBody LoginRequest corpo) {
		return authService.login(corpo);
	}

	/** Chi sono: utile al frontend per ripristinare lo stato dopo un refresh. */
	@GetMapping("/io")
	public UtenteAutenticato io(@UtenteCorrente UtenteAutenticato utente) {
		return utente;
	}
}
