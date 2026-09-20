package com.flamingo.qa.api.model.auth;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Two shapes, both carrying HTTP 200: {@code {"token":"..."}} or
 * {@code {"reason":"Bad credentials"}}. One model, because the caller has to read the body
 * to find out which it got.
 */
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
