package com.smarttask.user.service;

import com.smarttask.user.dto.AuthenticationResult;
import com.smarttask.user.dto.LoginRequest;
import com.smarttask.user.dto.RegisterRequest;

public interface UserService {

	AuthenticationResult register(RegisterRequest request);

	AuthenticationResult login(LoginRequest request);

	AuthenticationResult refresh(String refreshToken);
}
