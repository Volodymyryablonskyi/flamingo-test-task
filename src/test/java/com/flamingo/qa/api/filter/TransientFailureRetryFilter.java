package com.flamingo.qa.api.filter;

import com.flamingo.qa.core.config.Config;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retries genuinely transient transport failures - 5xx, 429 and connection errors - with
 * exponential backoff.
 *
 * <p><strong>Why this lives in an HTTP filter and not in a JUnit retrying extension.</strong>
 * A test-level retry re-runs the whole method, assertions included, so a real intermittent
 * bug gets papered over by the second attempt and the report claims a pass. Retrying inside
 * the transport means only the request is repeated; the assertions still run exactly once,
 * against the response that finally arrived, and an assertion failure is fatal on the first
 * attempt as it should be.
 *
 * <p>Deliberately narrow: 4xx other than 429 are the server telling us the request was
 * wrong, and repeating it would just be noise against a public service.
 */
public class TransientFailureRetryFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TransientFailureRetryFilter.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final int maxAttempts;
    private final long backoffMillis;

    public TransientFailureRetryFilter() {
        this(Config.apiRetryMaxAttempts(), Config.apiRetryBackoff().toMillis());
    }

    TransientFailureRetryFilter(int maxAttempts, long backoffMillis) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMillis = backoffMillis;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        RuntimeException lastTransportFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Response response = ctx.next(requestSpec, responseSpec);
                if (!isTransient(response.statusCode()) || attempt == maxAttempts) {
                    return response;
                }
                log.warn("{} {} returned HTTP {} - retrying (attempt {} of {})",
                        requestSpec.getMethod(), requestSpec.getURI(), response.statusCode(),
                        attempt, maxAttempts);
            } catch (RuntimeException transportFailure) {
                // Connection refused, read timeout, DNS failure: REST Assured wraps these.
                if (attempt == maxAttempts) {
                    throw transportFailure;
                }
                lastTransportFailure = transportFailure;
                log.warn("{} {} failed with {} - retrying (attempt {} of {})",
                        requestSpec.getMethod(), requestSpec.getURI(),
                        transportFailure.getClass().getSimpleName(), attempt, maxAttempts);
            }
            backOff(attempt);
        }

        // Unreachable: the loop either returns or rethrows on the final attempt.
        throw lastTransportFailure != null
                ? lastTransportFailure
                : new IllegalStateException("Retry loop exhausted without a response.");
    }

    private static boolean isTransient(int statusCode) {
        return statusCode >= 500 || statusCode == TOO_MANY_REQUESTS;
    }

    /**
     * Exponential: 1x, 2x, 4x the configured backoff. Keeps pressure off a struggling service.
     *
     * <p>The only {@code Thread.sleep} in the suite. It is a backoff between transport
     * retries, not a wait for application state - waiting for state is what Playwright's
     * auto-waiting and web-first assertions are for, and sleeping for that is banned here.
     */
    private void backOff(int completedAttempts) {
        try {
            Thread.sleep(backoffMillis * (1L << (completedAttempts - 1)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before a retry.", e);
        }
    }
}
