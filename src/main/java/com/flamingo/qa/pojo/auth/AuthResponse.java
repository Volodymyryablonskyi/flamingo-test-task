package com.flamingo.qa.pojo.auth;

public record AuthResponse(String token, String reason) {

    public boolean isSuccessful() {
        return token != null && !token.isBlank();
    }
}
