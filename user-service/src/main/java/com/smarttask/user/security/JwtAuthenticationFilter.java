package com.smarttask.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final SimpleGrantedAuthority USER_AUTHORITY = new SimpleGrantedAuthority("ROLE_USER");

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (isPublicEndpoint(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);
		if (!jwtService.isValidAccessToken(token)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String email = jwtService.extractEmail(token);
		Collection<SimpleGrantedAuthority> authorities =
				Collections.singletonList(USER_AUTHORITY);
		var authentication =
				new UsernamePasswordAuthenticationToken(email, null, authorities);
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	/** Matches public routes from the development plan (auth, OpenAPI, health). */
	private boolean isPublicEndpoint(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null || path.isEmpty()) {
			path = request.getRequestURI();
		}
		return path.startsWith("/api/auth")
				|| path.startsWith("/swagger-ui")
				|| path.startsWith("/v3/api-docs")
				|| path.startsWith("/api-docs")
				|| path.equals("/actuator/health")
				|| path.startsWith("/actuator/health/");
	}
}
