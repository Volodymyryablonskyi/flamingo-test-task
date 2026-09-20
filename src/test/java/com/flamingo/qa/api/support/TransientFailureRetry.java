package com.flamingo.qa.api.support;

import com.flamingo.qa.core.config.Config;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Re-sends a request that failed for a genuinely transient reason - 5xx, 429, or a
 * connection/read failure - with exponential backoff.
 *
 * <p><strong>Why this is not a JUnit retrying extension.</strong> A test-level retry re-runs
 * the whole method, assertions included, so a real intermittent bug gets papered over by the
 * second attempt and the report claims a pass. Here only the HTTP call is repeated: it is
 * invoked by the client, below the response-spec validation and far below the test, so
 * assertions still run exactly once against whatever response finally arrived, and an
 * assertion failure stays fatal on the first attempt.
 *
 * <p><strong>Why this is not a REST Assured {@code Filter} either</strong>, which is the
 * obvious place for it and where this started. {@code FilterContext.next()} walks a
 * single-use iterator over the filter chain: calling it a second time runs off the end and
 * returns {@code null} rather than re-sending, so a retry filter appears to work, silently
 * turns the first retry into a {@code NullPointerException}, and is measurably worse than
 * no retry at all. Measured, not assumed - see {@code TransportResilienceTest}.
 *
 * <p>Deliberately narrow: a 4xx other than 429 is the server saying the request was wrong,
 * and repeating it would only add noise against a public service.
 */
public final class TransientFailureRetry {

    private static final Logger log = LoggerFactory.getLogger(TransientFailureRetry.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private TransientFailureRetry() {
    }

    /** Sends with the retry policy from {@link Config}. */
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
                // Exception, not RuntimeException: a read timeout surfaces as a checked
                // SocketTimeoutException thrown through REST Assured's Groovy internals,
                // and a RuntimeException catch lets it straight past - missing the one
                // failure this class most needs to cover.
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

    private static boolean isTransient(int statusCode) {
        return statusCode >= 500 || statusCode == TOO_MANY_REQUESTS;
    }

    /** Keeps the original exception as the cause so the stack still points at the socket. */
    private static RuntimeException asUnchecked(Exception failure, int attempts) {
        if (failure instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        return new IllegalStateException(
                "Request failed after " + attempts + " attempt(s): " + failure, failure);
    }

    /**
     * Exponential: 1x, 2x, 4x the configured backoff, to keep pressure off a struggling
     * service.
     *
     * <p>The only {@code Thread.sleep} in the suite, and it is a backoff between transport
     * retries - never a wait for application state. Waiting for state is what Playwright's
     * auto-waiting and web-first assertions are for, and sleeping for that is banned here.
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
