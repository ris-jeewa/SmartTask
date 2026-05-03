package com.smarttask.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	private String secret;
	private long accessTokenExpiration = 900_000L;
	private long refreshTokenExpiration = 604_800_000L;
	private String refreshTokenCookieName = "refreshToken";
	private boolean cookieSecure;
	private String cookiePath = "/";
}
