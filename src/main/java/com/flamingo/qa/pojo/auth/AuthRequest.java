package com.flamingo.qa.pojo.auth;

import lombok.Builder;

@Builder
public record AuthRequest(String username, String password) {
}
