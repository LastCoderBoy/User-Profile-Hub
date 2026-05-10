package com.jk.User_Profile_Hub.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;

    // Snapshot of the authenticated user's public profile
    private UserResponse user;

    // ============================================
    // Factory Method
    // ============================================

    public static AuthResponse of(String accessToken,
                                  long expiresIn,
                                  UserResponse user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
