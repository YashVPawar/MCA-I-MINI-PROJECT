package com.medroute.auth;

public record AuthResponse(String accessToken, String tokenType, String username,
                           String displayName, String role, String hospitalId) {
}