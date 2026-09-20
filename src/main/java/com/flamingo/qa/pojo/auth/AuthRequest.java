package com.flamingo.qa.pojo.auth;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Credentials for {@code POST /auth}. */
@Value
@Builder
@Jacksonized
public class AuthRequest {

    String username;
    String password;
}
