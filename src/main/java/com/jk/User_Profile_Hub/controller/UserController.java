package com.jk.User_Profile_Hub.controller;

import com.jk.User_Profile_Hub.dto.ApiResponse;
import com.jk.User_Profile_Hub.dto.request.UpdateProfileRequest;
import com.jk.User_Profile_Hub.dto.response.UserResponse;
import com.jk.User_Profile_Hub.security.UserPrincipal;
import com.jk.User_Profile_Hub.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.jk.User_Profile_Hub.utils.AppConstants.PROFILE_PATH;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(PROFILE_PATH) // The endpoint is authenticated
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("[USER-CONTROLLER] Get profile request for user: {}", userPrincipal.getUsername());

        UserResponse response = userService.getCurrentUserProfile(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", response));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("[USER-CONTROLLER] Update profile request for user: {}", userPrincipal.getUsername());

        UserResponse response = userService.updateCurrentUserProfile(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("User profile updated successfully", response));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUserProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("[USER-CONTROLLER] Delete profile request for user: {}", userPrincipal.getUsername());

        userService.deleteCurrentUserProfile(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("User profile deleted successfully"));
    }
}
