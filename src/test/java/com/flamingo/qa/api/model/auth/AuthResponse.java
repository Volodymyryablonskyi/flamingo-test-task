package com.flamingo.qa.api.model.auth;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * The body of {@code POST /auth}, which is one of two shapes depending on the outcome -
 * and, notably, carries HTTP 200 either way.
 *
 * <ul>
 *   <li>success: {@code {"token":"abc123def456789"}}</li>
 *   <li>failure: {@code {"reason":"Bad credentials"}}</li>
 * </ul>
 *
 * Both fields live on one model because the caller has to look at the body to find out
 * which it got; the status code does not tell them.
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
