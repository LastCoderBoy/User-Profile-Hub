package com.jk.User_Profile_Hub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    // Snapshot of the authenticated user's public profile
    private UserResponse user;

    // ============================================
    // Factory Method
    // ============================================

    public static AuthResponse of(String accessToken,
                                  String refreshToken,
                                  long expiresIn,
                                  UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
