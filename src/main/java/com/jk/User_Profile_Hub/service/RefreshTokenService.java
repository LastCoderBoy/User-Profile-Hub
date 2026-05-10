package com.jk.User_Profile_Hub.service;


import com.jk.User_Profile_Hub.entity.RefreshToken;
import com.jk.User_Profile_Hub.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user, String clientIP, String userAgent);

    RefreshToken findByToken(String token);

    RefreshToken verifyRefreshToken(String token);

    RefreshToken rotateRefreshToken(RefreshToken oldToken, String clientIP, String userAgent);

    void revokeRefreshToken(String token);

    void revokeAllRefreshTokensAsync(Long userId);
}
