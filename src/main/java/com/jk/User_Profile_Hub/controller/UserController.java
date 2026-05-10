package com.jk.User_Profile_Hub.controller;

import com.jk.User_Profile_Hub.dto.ApiResponse;
import com.jk.User_Profile_Hub.dto.request.RegisterRequest;
import com.jk.User_Profile_Hub.dto.response.AuthResponse;
import com.jk.User_Profile_Hub.dto.response.UserResponse;
import com.jk.User_Profile_Hub.entity.UserPrincipal;
import com.jk.User_Profile_Hub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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



    }

    // updateUserProfile()

    // deleteUserProfile()
}
