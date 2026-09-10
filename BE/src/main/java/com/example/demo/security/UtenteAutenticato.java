package com.example.demo.security;

import com.example.demo.enums.Ruolo;

import java.util.UUID;

/** Cio' che sappiamo di chi sta chiamando, ricavato dal token. Diventa il principal di Spring Security. */
public record UtenteAutenticato(UUID id, String username, Ruolo ruolo) {
}
