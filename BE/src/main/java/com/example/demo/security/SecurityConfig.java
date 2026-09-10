package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/** Cost factor 10: default di Spring e minimo raccomandato da OWASP (~68 ms su questa macchina). */
	private static final int BCRYPT_STRENGTH = 10;

	private final JwtAuthenticationFilter jwtFilter;
	private final GestoreAccessiNegati gestoreAccessiNegati;

	public SecurityConfig(JwtAuthenticationFilter jwtFilter, GestoreAccessiNegati gestoreAccessiNegati) {
		this.jwtFilter = jwtFilter;
		this.gestoreAccessiNegati = gestoreAccessiNegati;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				// CSRF protegge le sessioni a cookie: con un'API stateless a token non serve
				.csrf(csrf -> csrf.disable())
				// nessuna sessione lato server: l'identita' sta tutta nel token
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// il preflight del browser viaggia senza token
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// solo registrazione e login sono pubbliche: /api/auth/io no
						.requestMatchers("/api/auth/registrazione", "/api/auth/login").permitAll()
						// l'handshake WebSocket si autentica da solo (JwtHandshakeInterceptor)
						.requestMatchers("/ws/**").permitAll()
						// creare notifiche e' un'operazione amministrativa
						.requestMatchers(HttpMethod.POST, "/api/notifiche").hasRole("ADMIN")
						.anyRequest().authenticated())
				// senza questo, chi non ha token riceverebbe 403 invece di 401
				.exceptionHandling(e -> e
						.authenticationEntryPoint(gestoreAccessiNegati)
						.accessDeniedHandler(gestoreAccessiNegati))
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Unica sorgente CORS dell'applicazione: la usa Spring Security, che con i suoi
	 * filtri viene prima di Spring MVC.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
