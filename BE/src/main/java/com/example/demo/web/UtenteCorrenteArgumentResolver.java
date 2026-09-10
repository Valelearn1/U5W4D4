package com.example.demo.web;

import com.example.demo.security.UtenteAutenticato;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Ricava l'utente corrente dal SecurityContext, dove l'ha depositato JwtAuthenticationFilter
 * dopo aver verificato la firma del token.
 *
 * Prima leggeva l'header X-Utente-Id, che il client poteva inventarsi: era la falla
 * piu' grossa del progetto ed e' chiusa qui.
 */
public class UtenteCorrenteArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(UtenteCorrente.class)
				&& (UUID.class.equals(parameter.getParameterType())
				|| UtenteAutenticato.class.equals(parameter.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter,
	                              ModelAndViewContainer mavContainer,
	                              NativeWebRequest webRequest,
	                              WebDataBinderFactory binderFactory) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof UtenteAutenticato utente)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token mancante o non valido");
		}
		return UUID.class.equals(parameter.getParameterType()) ? utente.id() : utente;
	}
}
