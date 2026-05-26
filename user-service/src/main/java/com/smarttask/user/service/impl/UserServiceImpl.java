package com.smarttask.user.service.impl;

import com.smarttask.user.domain.User;
import com.smarttask.user.dto.AuthenticationResult;
import com.smarttask.user.dto.LoginRequest;
import com.smarttask.user.dto.RegisterRequest;
import com.smarttask.user.exception.DuplicateRegistrationException;
import com.smarttask.user.mapper.UserMapper;
import com.smarttask.user.repository.UserRepository;
import com.smarttask.user.security.JwtService;
import com.smarttask.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Override
	@Transactional
	public AuthenticationResult register(RegisterRequest request) {
		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new DuplicateRegistrationException("Email already registered");
		}
		if (userRepository.findByUsername(request.username()).isPresent()) {
			throw new DuplicateRegistrationException("Username already taken");
		}

		User user = userMapper.toEntity(request);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		userRepository.save(user);
		return issueTokens(user);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthenticationResult login(LoginRequest request) {
		User user = userRepository
				.findByEmail(request.email())
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadCredentialsException("Invalid email or password");
		}
		return issueTokens(user);
	}

	@Override
	@Transactional(readOnly = true)
	public AuthenticationResult refresh(String refreshToken) {
		if (refreshToken == null
				|| refreshToken.isBlank()
				|| !jwtService.isValidRefreshToken(refreshToken)) {
			throw new BadCredentialsException("Invalid or expired refresh token");
		}
		String email = jwtService.extractEmail(refreshToken);
		User user = userRepository
				.findByEmail(email)
				.orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
		return issueTokens(user);
	}

	private AuthenticationResult issueTokens(User user) {
		String access = jwtService.generateAccessToken(user.getEmail(), user.getUsername());
		String refresh = jwtService.generateRefreshToken(user.getEmail());
		return new AuthenticationResult(access, refresh);
	}
}
