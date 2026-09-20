package com.flamingo.qa.api.client;

import com.flamingo.qa.core.config.Config;
import com.flamingo.qa.core.config.ConfigurationException;
import com.flamingo.qa.api.model.auth.AuthResponse;

/**
 * Supplies the auth token, fetching it <strong>once per JVM</strong>.
 *
 * <p>Authenticating per test would mean a dozen-odd pointless round trips to a shared
 * public service the brief asks us not to overload, and it would put an unrelated network
 * call in front of every assertion.
 *
 * <p>Thread-safe by necessity, not by habit: test classes run concurrently, so without the
 * double-checked lock several threads would race to authenticate at the same moment.
 */
public final class TokenProvider {

    private static final Object LOCK = new Object();
    private static final AuthClient AUTH_CLIENT = new AuthClient();

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
        AuthResponse response = AUTH_CLIENT.requestToken(Config.apiUsername(), Config.apiPassword());
        if (!response.isSuccessful()) {
            throw new ConfigurationException(
                    "Could not authenticate against " + Config.apiBaseUrl()
                            + " as user '" + Config.apiUsername() + "': "
                            + (response.getReason() == null ? "no token in the response" : response.getReason()));
        }
        return response.getToken();
    }
}
