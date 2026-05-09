package com.jk.User_Profile_Hub.utils;


import com.jk.User_Profile_Hub.exception.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Base64;

import static com.jk.User_Profile_Hub.utils.AppConstants.BEARER_PREFIX_LENGTH;

/**
 * Utility class for JWT token operations
 *
 * @author LastCoderBoy
 * @since 1.0.0
 */
@Slf4j
public class TokenUtils {
    private TokenUtils() {
        throw new UnsupportedOperationException("TokenUtils is a utility class and cannot be instantiated");
    }

    public static String validateAndExtractToken(String authorizationHeader) {
        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Authorization header is missing or invalid");
            throw new InvalidTokenException("Missing or Invalid authorization header");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX_LENGTH);
        if(token.isBlank()) {
            log.error("[TOKEN-UTILS]: Token is blank");
            throw new InvalidTokenException("Token is blank");
        }
        return token;
    }

    public static String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32]; // 32 bytes = 256 bits
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}

