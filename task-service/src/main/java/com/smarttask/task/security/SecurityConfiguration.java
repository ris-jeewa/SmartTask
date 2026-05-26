package com.smarttask.task.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex.authenticationEntryPoint(
								(req, res, authException) -> res.sendError(
										HttpServletResponse.SC_UNAUTHORIZED))
						.accessDeniedHandler(
								(req, res, accessDeniedException) -> res.sendError(
										HttpServletResponse.SC_FORBIDDEN)))
				.authorizeHttpRequests(auth -> auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html")
						.permitAll()
						.requestMatchers("/v3/api-docs/**", "/api-docs/**")
						.permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**")
						.permitAll()
						.anyRequest()
						.authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
