package com.flamingo.qa.api.support;

import com.flamingo.qa.core.config.Config;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Re-sends a request that failed transiently - 5xx, 429, or a connection/read failure -
 * with exponential backoff.
 *
 * <p>It lives here rather than in a JUnit retrying extension because a test-level retry
 * re-runs assertions and can mask a real intermittent bug. Clients call it below the
 * response-spec validation, so assertions still run exactly once.
 *
 * <p>It is also not a REST Assured {@code Filter}, which is the obvious place and where
 * this started: {@code FilterContext.next()} walks a single-use iterator, so the second
 * call returns {@code null} instead of re-sending. See {@code TransportResilienceTest}.
 */
public final class TransientFailureRetry {

    private static final Logger log = LoggerFactory.getLogger(TransientFailureRetry.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private TransientFailureRetry() {
    }

    public static Response send(Supplier<Response> request) {
        return send(request, Config.apiRetryMaxAttempts(), Config.apiRetryBackoff().toMillis());
    }

    static Response send(Supplier<Response> request, int maxAttempts, long backoffMillis) {
        int attempts = Math.max(1, maxAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                Response response = request.get();
                if (!isTransient(response.statusCode()) || attempt == attempts) {
                    return response;
                }
                log.warn("Request returned HTTP {} - retrying (attempt {} of {})",
                        response.statusCode(), attempt, attempts);
            } catch (Exception transportFailure) {
                // Exception, not RuntimeException: a read timeout arrives as a checked
                // SocketTimeoutException thrown through REST Assured's Groovy internals.
                if (attempt == attempts) {
                    throw asUnchecked(transportFailure, attempts);
                }
                log.warn("Request failed with {} - retrying (attempt {} of {})",
                        transportFailure.getClass().getSimpleName(), attempt, attempts);
            }
            backOff(attempt, backoffMillis);
        }

        throw new IllegalStateException("Retry loop exhausted without a response.");
    }

    /** A 4xx other than 429 means the request was wrong; repeating it is just noise. */
    private static boolean isTransient(int statusCode) {
        return statusCode >= 500 || statusCode == TOO_MANY_REQUESTS;
    }

    private static RuntimeException asUnchecked(Exception failure, int attempts) {
        if (failure instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        return new IllegalStateException(
                "Request failed after " + attempts + " attempt(s): " + failure, failure);
    }

    /**
     * The only {@code Thread.sleep} in the suite, and it is a backoff between transport
     * retries - never a wait for application state.
     */
    private static void backOff(int completedAttempts, long backoffMillis) {
        try {
            Thread.sleep(backoffMillis * (1L << (completedAttempts - 1)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before a retry.", e);
        }
    }
}
