package com.flamingo.qa.clients;

import com.flamingo.qa.config.Config;
import com.flamingo.qa.config.ConfigurationException;
import com.flamingo.qa.pojo.auth.AuthResponse;

/**
 * Supplies the auth token, fetching it once per JVM rather than once per test against a
 * shared public service. Thread-safe by necessity: test classes run concurrently.
 */
public final class TokenProvider {

    private static final Object LOCK = new Object();
    private static final AuthApiClient AUTH_CLIENT = new AuthApiClient();

    private static volatile String token;

    private TokenProvider() {
    }

    public static String token() {
        String cached = token;
        if (cached == null) {
            synchronized (LOCK) {
                if (token == null) {
                    token = fetch();
                }
                cached = token;
            }
        }
        return cached;
    }

    private static String fetch() {
        AuthResponse response = AUTH_CLIENT
                .requestToken(Config.apiUsername(), Config.apiPassword())
                .asPojo(AuthResponse.class);

        if (!response.isSuccessful()) {
            throw new ConfigurationException(
                    "Could not authenticate against " + Config.apiBaseUrl()
                            + " as user '" + Config.apiUsername() + "': "
                            + (response.getReason() == null ? "no token in the response" : response.getReason()));
        }
        return response.getToken();
    }
}
