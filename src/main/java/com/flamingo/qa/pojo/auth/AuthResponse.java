package com.flamingo.qa.pojo.auth;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AuthResponse {

    String token;
    String reason;

    public boolean isSuccessful() {
        return token != null && !token.isBlank();
    }
}
