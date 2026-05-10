package com.jk.User_Profile_Hub.security;


public record JwtClaimsPayload(
        String email,
        String roles
) {}