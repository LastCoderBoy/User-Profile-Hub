package com.jk.User_Profile_Hub.service;

import com.jk.User_Profile_Hub.dto.request.LoginRequest;
import com.jk.User_Profile_Hub.dto.request.RegisterRequest;
import com.jk.User_Profile_Hub.dto.response.AuthResponse;
import com.jk.User_Profile_Hub.entity.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest registerRequest,
                          HttpServletResponse response,
                          HttpServletRequest request);

    AuthResponse login(LoginRequest loginRequest,
                       HttpServletRequest request,
                       HttpServletResponse response);

    void logout(UserPrincipal principal,
                HttpServletResponse response,
                HttpServletRequest request);

    AuthResponse refreshJwtTokens(HttpServletRequest request, HttpServletResponse response);
}
