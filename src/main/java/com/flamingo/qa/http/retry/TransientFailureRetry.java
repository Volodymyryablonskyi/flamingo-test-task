package com.flamingo.qa.http.retry;

import com.flamingo.qa.config.Config;
import com.flamingo.qa.http.response.StatusCode;
import com.flamingo.qa.util.CustomLogger;
import io.restassured.response.Response;

import java.util.function.Supplier;

public final class TransientFailureRetry {

    private static final CustomLogger log = CustomLogger.getLogger(TransientFailureRetry.class);

    private static final int TOO_MANY_REQUESTS = StatusCode.STATUS_429_TOO_MANY_REQUESTS.getCode();
    private static final int LOWEST_SERVER_ERROR = StatusCode.STATUS_500_INTERNAL_SERVER_ERROR.getCode();

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
        return statusCode >= LOWEST_SERVER_ERROR || statusCode == TOO_MANY_REQUESTS;
    }

    private static RuntimeException asUnchecked(Exception failure, int attempts) {
        if (failure instanceof RuntimeException runtimeFailure) {
            return runtimeFailure;
        }
        return new IllegalStateException(
                "Request failed after " + attempts + " attempt(s): " + failure, failure);
    }

    private static void backOff(int completedAttempts, long backoffMillis) {
        try {
            Thread.sleep(backoffMillis * (1L << (completedAttempts - 1)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while backing off before a retry.", e);
        }
    }
}
