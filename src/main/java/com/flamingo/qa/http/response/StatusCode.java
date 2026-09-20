package com.flamingo.qa.http.response;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * The HTTP status codes this suite asserts on.
 *
 * <p>Named constants instead of magic numbers: {@code hasStatusCode(STATUS_403_FORBIDDEN)}
 * says what is expected, and a failure message reads {@code 418 IM_A_TEAPOT} rather than a
 * bare integer.
 *
 * <p>Two entries look odd and are here deliberately, because both are measured behaviour of
 * Restful Booker: a successful DELETE answers {@link #STATUS_201_CREATED}, and any
 * multi-value {@code Accept} header answers {@link #STATUS_418_IM_A_TEAPOT}.
 */
@Getter
public enum StatusCode {

    STATUS_200_OK(200),
    STATUS_201_CREATED(201),
    STATUS_204_NO_CONTENT(204),
    STATUS_400_BAD_REQUEST(400),
    STATUS_401_UNAUTHORIZED(401),
    STATUS_403_FORBIDDEN(403),
    STATUS_404_NOT_FOUND(404),
    STATUS_405_METHOD_NOT_ALLOWED(405),
    STATUS_418_IM_A_TEAPOT(418),
    STATUS_429_TOO_MANY_REQUESTS(429),
    STATUS_500_INTERNAL_SERVER_ERROR(500),
    STATUS_502_BAD_GATEWAY(502),
    STATUS_503_SERVICE_UNAVAILABLE(503);

    private final int code;

    StatusCode(int code) {
        this.code = code;
    }

    public static Optional<StatusCode> of(int code) {
        return Arrays.stream(values()).filter(status -> status.code == code).findFirst();
    }

    /**
     * @throws IllegalArgumentException naming the code, so an unmapped status is a prompt to
     *         add a constant rather than a silent miss
     */
    public static StatusCode from(int code) {
        return of(code).orElseThrow(() -> new IllegalArgumentException(
                "HTTP status " + code + " has no StatusCode constant - add one."));
    }

    /** Never throws: used in assertion messages, where an unmapped code must still read well. */
    public static String describe(int code) {
        return of(code).map(status -> code + " " + status.name().replaceFirst("STATUS_[0-9]+_", ""))
                .orElse(code + " (unmapped)");
    }

    @Override
    public String toString() {
        return describe(code);
    }
}
