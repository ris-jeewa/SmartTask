package com.smarttask.task.security;

import com.smarttask.task.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

	public static final String CLAIM_TOKEN_TYPE = "typ";
	private static final String TYPE_ACCESS = "ACCESS";

	private final JwtProperties props;
	private volatile SecretKey signKey;

	public JwtService(JwtProperties props) {
		this.props = props;
	}

	private SecretKey getSignKey() {
		if (signKey == null) {
			String secret = props.getSecret();
			if (secret == null || secret.isBlank()) {
				throw new IllegalStateException("jwt.secret must be configured");
			}
			byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
			if (keyBytes.length < 32) {
				throw new IllegalStateException("jwt.secret must be at least 32 UTF-8 bytes for HS256");
			}
			signKey = Keys.hmacShaKeyFor(keyBytes);
		}
		return signKey;
	}

	public boolean isValidAccessToken(String token) {
		try {
			Claims claims = parseClaims(token);
			return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
		} catch (Exception e) {
			return false;
		}
	}

	public String extractEmail(String token) {
		return parseClaims(token).getSubject();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token).getPayload();
	}
}
