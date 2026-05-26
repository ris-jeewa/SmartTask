package com.smarttask.user.service.impl;

import com.smarttask.user.domain.Role;
import com.smarttask.user.domain.User;
import com.smarttask.user.dto.AuthenticationResult;
import com.smarttask.user.dto.LoginRequest;
import com.smarttask.user.dto.RegisterRequest;
import com.smarttask.user.exception.DuplicateRegistrationException;
import com.smarttask.user.mapper.UserMapper;
import com.smarttask.user.repository.UserRepository;
import com.smarttask.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserMapper userMapper;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private UserServiceImpl userService;

	private RegisterRequest registerRequest;
	private User mappedUser;

	@BeforeEach
	void setUp() {
		registerRequest = new RegisterRequest("jane", "jane@example.com", "password12");
		mappedUser =
				User.builder()
						.username("jane")
						.email("jane@example.com")
						.passwordHash(null)
						.role(Role.USER)
						.build();
	}

	@Test
	void register_succeeds_whenEmailAndUsernameAreFree() {
		when(userRepository.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
		when(userRepository.findByUsername(registerRequest.username())).thenReturn(Optional.empty());
		when(userMapper.toEntity(registerRequest)).thenReturn(mappedUser);
		when(passwordEncoder.encode(registerRequest.password())).thenReturn("{bcrypt}hash");
		when(userRepository.save(any(User.class)))
				.thenAnswer(
						inv -> {
							User u = inv.getArgument(0);
							u.setId(1L);
							return u;
						});
		when(jwtService.generateAccessToken(eq("jane@example.com"), eq("jane")))
				.thenReturn("access-jwt");
		when(jwtService.generateRefreshToken("jane@example.com")).thenReturn("refresh-jwt");

		AuthenticationResult result = userService.register(registerRequest);

		assertThat(result.accessToken()).isEqualTo("access-jwt");
		assertThat(result.refreshToken()).isEqualTo("refresh-jwt");
		verify(userRepository).save(any(User.class));
	}

	@Test
	void register_fails_whenEmailExists() {
		when(userRepository.findByEmail(registerRequest.email()))
				.thenReturn(Optional.of(User.builder().id(9L).build()));

		assertThatThrownBy(() -> userService.register(registerRequest))
				.isInstanceOf(DuplicateRegistrationException.class)
				.hasMessageContaining("Email");

		verify(userRepository, never()).save(any());
	}

	@Test
	void register_fails_whenUsernameExists() {
		when(userRepository.findByEmail(registerRequest.email())).thenReturn(Optional.empty());
		when(userRepository.findByUsername(registerRequest.username()))
				.thenReturn(Optional.of(User.builder().id(8L).build()));

		assertThatThrownBy(() -> userService.register(registerRequest))
				.isInstanceOf(DuplicateRegistrationException.class)
				.hasMessageContaining("Username");

		verify(userRepository, never()).save(any());
	}

	@Test
	void login_succeeds_whenCredentialsMatch() {
		User stored =
				User.builder()
						.id(1L)
						.username("jane")
						.email("jane@example.com")
						.passwordHash("{bcrypt}hash")
						.role(Role.USER)
						.build();
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(stored));
		when(passwordEncoder.matches("password12", "{bcrypt}hash")).thenReturn(true);
		when(jwtService.generateAccessToken("jane@example.com", "jane")).thenReturn("a");
		when(jwtService.generateRefreshToken("jane@example.com")).thenReturn("r");

		AuthenticationResult result = userService.login(new LoginRequest("jane@example.com", "password12"));

		assertThat(result.accessToken()).isEqualTo("a");
		assertThat(result.refreshToken()).isEqualTo("r");
	}

	@Test
	void login_fails_whenUserNotFound() {
		when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.login(new LoginRequest("x@y.com", "password12")))
				.isInstanceOf(BadCredentialsException.class);

		verify(jwtService, never()).generateAccessToken(any(), any());
	}

	@Test
	void login_fails_whenPasswordDoesNotMatch() {
		User stored =
				User.builder()
						.id(1L)
						.username("jane")
						.email("jane@example.com")
						.passwordHash("{bcrypt}hash")
						.role(Role.USER)
						.build();
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(stored));
		when(passwordEncoder.matches("wrong", "{bcrypt}hash")).thenReturn(false);

		assertThatThrownBy(() -> userService.login(new LoginRequest("jane@example.com", "wrong")))
				.isInstanceOf(BadCredentialsException.class);

		verify(jwtService, never()).generateAccessToken(any(), any());
	}

	@Test
	void refresh_succeeds_whenRefreshTokenValid() {
		when(jwtService.isValidRefreshToken("rt")).thenReturn(true);
		when(jwtService.extractEmail("rt")).thenReturn("jane@example.com");
		User stored =
				User.builder()
						.id(2L)
						.username("jane")
						.email("jane@example.com")
						.passwordHash("h")
						.role(Role.USER)
						.build();
		when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(stored));
		when(jwtService.generateAccessToken("jane@example.com", "jane")).thenReturn("new-a");
		when(jwtService.generateRefreshToken("jane@example.com")).thenReturn("new-r");

		AuthenticationResult result = userService.refresh("rt");

		assertThat(result.accessToken()).isEqualTo("new-a");
		assertThat(result.refreshToken()).isEqualTo("new-r");
	}

	@Test
	void refresh_fails_whenRefreshInvalid() {
		when(jwtService.isValidRefreshToken("bad")).thenReturn(false);

		assertThatThrownBy(() -> userService.refresh("bad")).isInstanceOf(BadCredentialsException.class);

		verify(jwtService, never()).extractEmail(any());
		verify(userRepository, never()).findByEmail(any());
	}
}
