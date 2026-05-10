package com.jk.User_Profile_Hub.controller;

import com.jk.User_Profile_Hub.dto.ApiResponse;
import com.jk.User_Profile_Hub.dto.request.LoginRequest;
import com.jk.User_Profile_Hub.dto.request.RegisterRequest;
import com.jk.User_Profile_Hub.dto.response.AuthResponse;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.jk.User_Profile_Hub.utils.AppConstants.AUTH_PATH;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AUTH_PATH)
public class AuthorizationController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                              HttpServletResponse response,
                                                              HttpServletRequest request) {
        // Base validation is done via @Valid annotation
        // processing the request to the service layer
        log.info("[AUTH-CONTROLLER] Registering user: {}", registerRequest.getEmail());

        AuthResponse authResponse = authService.register(registerRequest, response, request);

        return ResponseEntity.ok(
                ApiResponse.success("User registered successfully", authResponse)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        log.info("[AUTH-CONTROLLER] Logging in user: {}", loginRequest.getEmail());

        AuthResponse authResponse = authService.login(loginRequest, request, response);
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal,
                                                    HttpServletResponse response, // used for clearing the cookies
                                                    HttpServletRequest request) {
        log.info("[AUTH-CONTROLLER] Logging out user...");

        authService.logout(principal, response, request);
        return ResponseEntity.ok(ApiResponse.success("User logged out successfully"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshJwtToken(HttpServletRequest request,
                                                                     HttpServletResponse response) {
        log.info("[AUTH-CONTROLLER] Refreshing JWT tokens...");
        AuthResponse authResponse = authService.refreshJwtTokens(request, response);

        return ResponseEntity.ok(
                ApiResponse.success("JWT tokens refreshed successfully", authResponse)
        );
    }
}
