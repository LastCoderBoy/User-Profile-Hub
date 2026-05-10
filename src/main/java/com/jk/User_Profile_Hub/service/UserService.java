package com.jk.User_Profile_Hub.service;

import com.jk.User_Profile_Hub.dto.request.UpdateProfileRequest;
import com.jk.User_Profile_Hub.dto.response.UserResponse;
import com.jk.User_Profile_Hub.security.UserPrincipal;

public interface UserService {

    UserResponse getCurrentUserProfile(UserPrincipal principal);

    UserResponse updateCurrentUserProfile(UserPrincipal principal, UpdateProfileRequest request);

    void deleteCurrentUserProfile(UserPrincipal principal);
}
