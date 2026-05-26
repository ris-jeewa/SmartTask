package com.smarttask.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI userServiceOpenApi() {
		return new OpenAPI()
				.components(
						new Components()
								.addSecuritySchemes(
										"bearerJwt",
										new SecurityScheme()
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.in(SecurityScheme.In.HEADER)
												.name(HttpHeaders.AUTHORIZATION)))
				.info(
						new Info()
								.title("SmartTask User Service")
								.description("Registration, login, and refresh token endpoints")
								.version("1.0.0"));
	}
}
