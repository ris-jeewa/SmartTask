package com.smarttask.user.security;

import com.smarttask.user.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

	public static final String CLAIM_TOKEN_TYPE = "typ";
	private static final String TYPE_ACCESS = "ACCESS";
	private static final String TYPE_REFRESH = "REFRESH";

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

	public String generateAccessToken(String email, String username) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + props.getAccessTokenExpiration());
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(email)
				.claim("username", username)
				.claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(getSignKey())
				.compact();
	}

	public String generateRefreshToken(String email) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + props.getRefreshTokenExpiration());
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(email)
				.claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(getSignKey())
				.compact();
	}

	public boolean isValidAccessToken(String token) {
		try {
			Claims claims = parseClaims(token);
			return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isValidRefreshToken(String token) {
		try {
			Claims claims = parseClaims(token);
			return TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
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
