package com.jk.User_Profile_Hub.security;


import java.util.List;

public record JwtClaimsPayload(
        String email,
        List<String> roles
) {}