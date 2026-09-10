package com.example.demo.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Involucro esplicito per le risposte paginate.
 *
 * Serializzare direttamente un Page di Spring Data e' sconsigliato: la sua forma JSON
 * non fa parte del contratto pubblico e puo' cambiare fra le versioni della libreria.
 * Con un record nostro il JSON che vede il frontend e' deciso da noi.
 */
public record PaginaResponse<T>(
		List<T> contenuto,
		int pagina,
		int dimensione,
		long totaleElementi,
		int totalePagine,
		boolean ultima
) {
	public static <E, T> PaginaResponse<T> da(Page<E> page, Function<E, T> mappatore) {
		return new PaginaResponse<>(
				page.getContent().stream().map(mappatore).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.isLast()
		);
	}

	public static <T> PaginaResponse<T> da(Page<T> page) {
		return da(page, Function.identity());
	}
}
