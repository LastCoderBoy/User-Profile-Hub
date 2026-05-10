package com.jk.User_Profile_Hub.service.impl;

import com.jk.User_Profile_Hub.dto.request.UpdateProfileRequest;
import com.jk.User_Profile_Hub.dto.response.UserResponse;
import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.exception.InvalidTokenException;
import com.jk.User_Profile_Hub.exception.ResourceNotFoundException;
import com.jk.User_Profile_Hub.redis.RedisService;
import com.jk.User_Profile_Hub.repository.UserRepository;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.service.RefreshTokenService;
import com.jk.User_Profile_Hub.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UserPrincipal principal) {
        Long userId = extractUserId(principal);

        UserResponse cachedProfile = redisService.getCachedUserProfile(userId);
        if (cachedProfile != null) {
            log.info("[USER-SERVICE] Returning user profile from cache for user ID: {}", userId);
            return cachedProfile;
        }

        User user = findActiveUserById(userId);
        UserResponse response = UserResponse.from(user);
        redisService.cacheUserProfile(userId, response);
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateCurrentUserProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = findActiveUserById(extractUserId(principal));

        user.updateProfile(
                request.getFirstName(),
                request.getLastName(),
                request.getTitle(),
                request.getSummary(),
                request.getPhoneNumber(),
                request.getLocation(),
                request.getLinkedinUrl(),
                request.getWebsiteUrl()
        );

        User updatedUser = userRepository.save(user);
        log.info("[USER-SERVICE] Updated user profile for user ID: {}", updatedUser.getId());

        UserResponse response = UserResponse.from(updatedUser);
        redisService.invalidateUserProfile(updatedUser.getId());
        redisService.cacheUserProfile(updatedUser.getId(), response);
        return response;
    }

    @Override
    @Transactional
    public void deleteCurrentUserProfile(UserPrincipal principal) {
        User user = findUserById(extractUserId(principal));

        if (user.isDeleted()) {
            redisService.invalidateUserProfile(user.getId());
            redisService.invalidateUserPrincipal(user.getEmail());
            return;
        }

        user.softDelete();
        userRepository.save(user);
        log.info("[USER-SERVICE] Deleted user profile for user ID: {}", user.getId());

        refreshTokenService.revokeAllRefreshTokensAsync(user.getId());
        redisService.invalidateUserProfile(user.getId());
        redisService.invalidateUserPrincipal(user.getEmail());
    }

    // =============================================
    //              HELPER METHODS
    // =============================================

    private Long extractUserId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new InvalidTokenException("Authenticated user is missing");
        }
        return principal.getId();
    }

    private User findActiveUserById(Long userId) {
        User user = findUserById(userId);
        if (!user.isActive()) {
            throw new ResourceNotFoundException("User account is not active");
        }
        return user;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
