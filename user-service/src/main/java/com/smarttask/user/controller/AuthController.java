package com.smarttask.user.controller;

import com.smarttask.user.config.JwtProperties;
import com.smarttask.user.dto.AuthResponse;
import com.smarttask.user.dto.LoginRequest;
import com.smarttask.user.dto.RegisterRequest;
import com.smarttask.user.security.RefreshCookieAppender;
import com.smarttask.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final JwtProperties jwtProperties;
	private final RefreshCookieAppender refreshCookieAppender;

	@PostMapping("/register")
	public AuthResponse register(
			@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
		var result = userService.register(request);
		refreshCookieAppender.attach(response, result.refreshToken());
		return toAuthResponse(result.accessToken());
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
		var result = userService.login(request);
		refreshCookieAppender.attach(response, result.refreshToken());
		return toAuthResponse(result.accessToken());
	}

	@PostMapping("/refresh")
	public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
		String rt = readRefreshTokenFromCookie(request);
		var result = userService.refresh(rt);
		refreshCookieAppender.attach(response, result.refreshToken());
		return toAuthResponse(result.accessToken());
	}

	private AuthResponse toAuthResponse(String accessToken) {
		long expiresInSeconds = jwtProperties.getAccessTokenExpiration() / 1000L;
		return new AuthResponse(accessToken, "Bearer", expiresInSeconds);
	}

	private String readRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		String name = jwtProperties.getRefreshTokenCookieName();
		for (Cookie c : cookies) {
			if (name.equals(c.getName())) {
				return c.getValue();
			}
		}
		return null;
	}
}
