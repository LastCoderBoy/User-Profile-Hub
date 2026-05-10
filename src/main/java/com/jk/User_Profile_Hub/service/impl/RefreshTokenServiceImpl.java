package com.jk.User_Profile_Hub.service.impl;

import com.jk.User_Profile_Hub.entity.RefreshToken;
import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.exception.custom.InternalServerException;
import com.jk.User_Profile_Hub.exception.custom.InvalidTokenException;
import com.jk.User_Profile_Hub.exception.custom.ResourceNotFoundException;
import com.jk.User_Profile_Hub.repository.RefreshTokenRepository;
import com.jk.User_Profile_Hub.service.RefreshTokenService;
import com.jk.User_Profile_Hub.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static com.jk.User_Profile_Hub.utils.AppConstants.REFRESH_TOKEN_DURATION_MS;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String clientIP, String userAgent) {
        // Generate a secure random token
        try {
            String tokenString = TokenUtils.generateSecureToken();

            RefreshToken refreshToken = RefreshToken.builder()
                    .token(tokenString)
                    .user(user)
                    .expiresAt(Instant.now().plusMillis(REFRESH_TOKEN_DURATION_MS))
                    .revoked(false)
                    .ipAddress(clientIP)
                    .userAgent(userAgent)
                    .build();

            refreshToken = refreshTokenRepository.save(refreshToken);
            log.info("[REFRESH-TOKEN-SERVICE] Created refresh token for user: {} (ID: {})",
                    user.getEmail(), user.getId());

            return refreshToken;
        } catch (Exception e) {
            log.error("[REFRESH-TOKEN-SERVICE] Failed to create refresh token: {}", e.getMessage());
            throw new InternalServerException("Failed to create internal token");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("[REFRESH-TOKEN-SERVICE] Refresh token not found");
                    return new ResourceNotFoundException("Invalid refresh token");
                });
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = findByToken(token);

        if (refreshToken.isExpired()) {
            log.warn("[REFRESH-TOKEN-SERVICE] Refresh token expired");
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        if (refreshToken.getRevoked()) {
            log.warn("[REFRESH-TOKEN-SERVICE] Refresh token has been revoked");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        return refreshToken;
    }


    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, String clientIP, String userAgent) {
        oldToken.revoke();
        refreshTokenRepository.save(oldToken);

        log.info("[REFRESH-TOKEN-SERVICE] Rotated refresh token for user: {}",
                oldToken.getUser().getEmail());

        return createRefreshToken(oldToken.getUser(), clientIP, userAgent);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = findByToken(token);
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        log.info("[REFRESH-TOKEN-SERVICE] Revoked refresh token for user: {}", refreshToken.getUser().getEmail());
    }

    /**
     * Revoke all refresh tokens for a user
     */
    @Override
    @Async("taskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllRefreshTokensAsync(Long userId) {
        try {
            int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
            log.info("[REFRESH-TOKEN-SERVICE] Revoked {} refresh tokens for user: {}", revokedCount, userId);
        } catch (Exception e) {
            log.error("[REFRESH-TOKEN-SERVICE] Error revoking refresh tokens for user {}: {}",
                    userId, e.getMessage(), e);
        }
    }
}
