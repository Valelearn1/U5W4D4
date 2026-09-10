package com.example.demo.web;

import java.lang.annotation.*;

/**
 * Inietta in un metodo di controller l'ID dell'utente autenticato.
 *
 * L'ID arriva dal JWT, verificato da JwtAuthenticationFilter e risolto da
 * UtenteCorrenteArgumentResolver. Il parametro puo' essere un UUID o un UtenteAutenticato.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UtenteCorrente {
}
