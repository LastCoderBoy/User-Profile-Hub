package com.jk.User_Profile_Hub.security;

import com.jk.User_Profile_Hub.exception.InvalidTokenException;
import com.jk.User_Profile_Hub.redis.RedisService;
import com.jk.User_Profile_Hub.utils.TokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import static com.jk.User_Profile_Hub.utils.AppConstants.AUTHORIZATION_HEADER;
import static com.jk.User_Profile_Hub.utils.AppConstants.PUBLIC_PATHS;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final JwtTokenProcessor jwtTokenProcessor;
    private final RedisService redisService;
    private final UserDetailsPersistence userDetailsPersistence;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Skip public paths
        if (isPublicPath(path)) {
            log.debug("[JWT-FILTER] Public path, skipping authentication: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        try{
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            String token = TokenUtils.validateAndExtractToken(authHeader);

            if(redisService.isTokenBlacklisted(token)){
                log.warn("[JWT-FILTER] Token is blacklisted");
                throw new InvalidTokenException("Token is blacklisted");
            }

            Optional<JwtClaimsPayload> claimsPayloadOpt = jwtTokenProcessor.validateAndExtractClaims(token);

            if(claimsPayloadOpt.isEmpty()){
                log.warn("[JWT-FILTER] Invalid JWT token");
                throw new InvalidTokenException("Invalid or expired token");
            }

            JwtClaimsPayload claimsPayload = claimsPayloadOpt.get();
            String email = claimsPayload.email();

            // UserPrincipal implements UserDetails
            UserDetails userDetails = userDetailsPersistence.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("[JWT-FILTER] Authentication successful for user: {}", email);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("[JWT-FILTER] Error while processing request: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream()
                .anyMatch(publicPath -> pathMatcher.match(publicPath, path));
    }
}
