package com.flamingo.qa.pojo.auth;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AuthRequest {

    String username;
    String password;
}
