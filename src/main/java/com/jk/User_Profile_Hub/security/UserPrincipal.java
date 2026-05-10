package com.jk.User_Profile_Hub.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jk.User_Profile_Hub.entity.User;
import com.jk.User_Profile_Hub.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
@Builder
public class UserPrincipal implements UserDetails {


    // ==================== Core Identity Fields ====================
    private final Long id;       // internal - never in URLs or responses
    private final String uuid;   // public - used in URLs and API responses
    private final String email;

    // ==================== Security Fields ====================
    @JsonIgnore
    private final String password;  // Only used during authentication

    @JsonIgnore
    private final Boolean isActive;

    @JsonIgnore
    private final Role userRole;


    /**
     * Create UserPrincipal from User entity (for login)
     * Used by CustomUserDetailsService
     */
    public static UserPrincipal create(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .password(user.getPasswordHash())
                .userRole(user.getRole())
                .isActive(user.isActive())
                .build();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(userRole.name())) ;
    }

    @Override
    public String getPassword() {return password;}

    @Override
    public String getUsername() {
        return email; // Spring Security treats this as the login identifier
    }

    @Override
    public boolean isAccountNonExpired() {return UserDetails.super.isAccountNonExpired();}

    @Override
    public boolean isAccountNonLocked() {return UserDetails.super.isAccountNonLocked();}

    @Override
    public boolean isCredentialsNonExpired() {return UserDetails.super.isCredentialsNonExpired();}

    @Override
    public boolean isEnabled() {return isActive;}

    // ==================== Helper Methods ====================

    public boolean isAdmin() {
        return userRole == Role.ROLE_ADMIN;
    }

    public String getUserRoleString() {
        return userRole.name();
    }
}
