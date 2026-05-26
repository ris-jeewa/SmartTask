package com.smarttask.user.security;

import com.smarttask.user.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RefreshCookieAppender {

	private final JwtProperties props;

	public void attach(HttpServletResponse response, String refreshToken) {
		Duration maxAge = Duration.ofMillis(props.getRefreshTokenExpiration());
		ResponseCookie cookie =
				ResponseCookie.from(props.getRefreshTokenCookieName(), refreshToken)
						.httpOnly(true)
						.secure(props.isCookieSecure())
						.path(props.getCookiePath())
						.maxAge(maxAge)
						.sameSite("Lax")
						.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
