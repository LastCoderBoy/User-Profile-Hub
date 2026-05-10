package com.jk.User_Profile_Hub.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static com.jk.User_Profile_Hub.utils.AppConstants.REFRESH_TOKEN_COOKIE_NAME;
import static com.jk.User_Profile_Hub.utils.AppConstants.REFRESH_TOKEN_DURATION_MS;

@Slf4j
@Component
public class CookiesManager {

    @Value("${cookie.secure}")
    private boolean isSecure;

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) (REFRESH_TOKEN_DURATION_MS / 1000)); // Convert to seconds
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        log.debug("[AUTH-COOKIE] Set refresh token cookie");
    }

    public Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();

        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        log.debug("[AUTH-COOKIE] Refresh token cookie cleared");
    }
}
