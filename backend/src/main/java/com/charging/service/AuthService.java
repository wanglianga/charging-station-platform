package com.charging.service;

import com.charging.dto.LoginRequest;
import com.charging.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
