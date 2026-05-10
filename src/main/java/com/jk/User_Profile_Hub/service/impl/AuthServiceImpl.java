package com.jk.User_Profile_Hub.service.impl;

import com.jk.User_Profile_Hub.config.CookiesManager;
import com.jk.User_Profile_Hub.dto.request.LoginRequest;
import com.jk.User_Profile_Hub.dto.request.RegisterRequest;
import com.jk.User_Profile_Hub.dto.response.AuthResponse;
import com.jk.User_Profile_Hub.dto.response.UserResponse;
import com.jk.User_Profile_Hub.entity.RefreshToken;
import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.exception.DuplicateResourceFoundException;
import com.jk.User_Profile_Hub.exception.InvalidTokenException;
import com.jk.User_Profile_Hub.exception.ResourceNotFoundException;
import com.jk.User_Profile_Hub.redis.RedisService;
import com.jk.User_Profile_Hub.repository.UserRepository;
import com.jk.User_Profile_Hub.security.JwtTokenProcessor;
import com.jk.User_Profile_Hub.service.AuthService;
import com.jk.User_Profile_Hub.service.RefreshTokenService;
import com.jk.User_Profile_Hub.utils.HeaderExtractor;
import com.jk.User_Profile_Hub.utils.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

import static com.jk.User_Profile_Hub.utils.AppConstants.ACCESS_TOKEN_DURATION_MS;
import static com.jk.User_Profile_Hub.utils.AppConstants.AUTHORIZATION_HEADER;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RedisService redisService;
    private final AuthenticationManager authenticationManager;
    private final CookiesManager cookiesManager;
    private final JwtTokenProcessor jwtTokenProcessor;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    // =========== REPOSITORIES ===========
    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)  // Rollback on ANY exception
    @Override
    public AuthResponse register(RegisterRequest registerRequest, HttpServletResponse httpResponse, HttpServletRequest httpRequest) {
        log.info("[AUTH-SERVICE] Starting registration for user: {}", registerRequest.getEmail());

        // Check email exist in our database or not
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("[AUTH-SERVICE] User with email {} already exists", registerRequest.getEmail());
            throw new DuplicateResourceFoundException("User with email " + registerRequest.getEmail() + " already exists");
        }

        // Create and Save User
        User newUser = User.createNew(
                registerRequest.getFirstName(),
                registerRequest.getLastName(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getPhoneNumber()
        );
        newUser = userRepository.save(newUser);
        log.info("[AUTH-SERVICE] User saved with ID: {}", newUser.getId());

        return issueAuthResponse(newUser,
                HeaderExtractor.extractClientIp(httpRequest),
                HeaderExtractor.extractUserAgent(httpRequest),
                httpResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("[AUTH-SERVICE] Authentication successful for user: {} (ID: {})",
                principal.getUsername(), principal.getId()); // username is email in our case

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));


        return issueAuthResponse(user,
                HeaderExtractor.extractClientIp(httpRequest),
                HeaderExtractor.extractUserAgent(httpRequest),
                httpResponse);
    }

    @Transactional
    @Override
    public void logout(UserPrincipal principal, HttpServletResponse response, HttpServletRequest request) {
        String email = principal != null ? principal.getUsername() : "unknown";
        try {
            // Revoke Refresh Token
            Optional<String> refreshToken = cookiesManager.extractRefreshTokenFromCookie(request);
            refreshToken.ifPresent(refreshTokenService::revokeRefreshToken);
        } catch (ResourceNotFoundException e) {
            // Token already removed/invalid in storage; logout stays idempotent.
            log.debug("[AUTH-SERVICE] Refresh token already invalid during logout: {}", e.getMessage());
        } catch (Exception e) {
            // Do not fail logout due to backend issues; always clear cookie.
            log.warn("[AUTH-SERVICE] Refresh token revoke failed during logout: {}", e.getMessage());
        }

        try {
            // Blacklist the Access Token in Redis
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            blacklistAccessToken(authHeader);
        } finally {
            cookiesManager.clearRefreshTokenCookie(response);
            log.info("[AUTH-SERVICE] Logout completed for user: {}", email);
        }
    }

    @Transactional
    @Override
    public AuthResponse refreshJwtTokens(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> optionalRefreshToken = cookiesManager.extractRefreshTokenFromCookie(request);
        if(optionalRefreshToken.isEmpty()){
            throw new InvalidTokenException("Request does not contain refresh token cookie");
        }
        // Extract Headers
        String clientIp = HeaderExtractor.extractClientIp(request);
        String userAgent = HeaderExtractor.extractUserAgent(request);

        // verify the old refresh token and create a new one
        String token = optionalRefreshToken.get();
        RefreshToken oldRefreshToken = refreshTokenService.verifyRefreshToken(token); // method will resolve the Lazy Exception

        User user = oldRefreshToken.getUser();
        if(!user.isActive()){
            log.warn("[AUTH-SERVICE] User with email {} is not active", user.getEmail());
            throw new DisabledException("Your account is not active. Please contact support for assistance.");
        }

        // Rotate the Refresh Token
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                oldRefreshToken,
                clientIp,
                userAgent
        );

        // Generate new Access Token
        String accessToken = jwtTokenProcessor.generateAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        // Set the Refresh token cookie
        cookiesManager.setRefreshTokenCookie(response, newRefreshToken.getToken());

        return AuthResponse.of(
                accessToken,
                ACCESS_TOKEN_DURATION_MS / 1000,
                UserResponse.from(user)
        );
    }


    // ====================================================
    //                    HELPER METHODS
    // ====================================================

    /**
     * Finalizes authentication for both register and login flows.
     * Generates access token, creates refresh token, sets cookie,
     * caches the user profile, and returns the auth response.
     */
    private AuthResponse issueAuthResponse(User user,
                                           String clientIp,
                                           String userAgent,
                                           HttpServletResponse response) {

        String accessToken = jwtTokenProcessor.generateAccessToken(
                user.getEmail(),
                user.getRole().name()
        );

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user, clientIp, userAgent
        );

        cookiesManager.setRefreshTokenCookie(response, refreshToken.getToken());

        UserResponse userResponse = UserResponse.from(user);
        redisService.cacheUserProfile(user.getId(), userResponse);

        return AuthResponse.of(
                accessToken,
                ACCESS_TOKEN_DURATION_MS / 1000,
                userResponse
        );
    }

    private void blacklistAccessToken(String authHeader){
        if(authHeader != null){
            try {
                String accessToken = TokenUtils.validateAndExtractToken(authHeader);
                Date tokenExpiration = jwtTokenProcessor.getExpirationDateFromToken(accessToken);
                long remainingTtl = tokenExpiration.getTime() - System.currentTimeMillis();

                if (remainingTtl > 0) {
                    redisService.blackListToken(accessToken, remainingTtl);
                    log.debug("[AUTH-SERVICE] Access token blacklisted for {}ms", remainingTtl);
                } else {
                    log.debug("[AUTH-SERVICE] Access token already expired, skipping blacklist");
                }
            } catch (InvalidTokenException e) {
                // Logout should be idempotent even with malformed/missing bearer token.
                log.debug("[AUTH-SERVICE] Skipping access token blacklist during logout: {}", e.getMessage());
            }
        }
    }
}
