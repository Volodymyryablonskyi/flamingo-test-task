package com.flamingo.qa.util;

import com.flamingo.qa.http.request.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class CustomLogger {

    private static final int MAX_BODY_LENGTH = 2_000;

    private final Logger logger;

    private CustomLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz.getSimpleName());
    }

    public static CustomLogger getLogger(Class<?> clazz) {
        return new CustomLogger(clazz);
    }

    public void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    public void logRequest(HttpMethod method, String uri, Map<String, ?> queryParams, Object body) {
        logger.debug("--> {} {}{}", method, uri, queryParams.isEmpty() ? "" : " " + queryParams);
        if (body != null) {
            logger.debug("--> body: {}", truncate(String.valueOf(body)));
        }
    }

    public void logResponse(int statusCode, String body) {
        logger.debug("<-- {} {}", statusCode, truncate(body));
    }

    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_BODY_LENGTH
                ? text.substring(0, MAX_BODY_LENGTH) + "... [truncated]"
                : text;
    }
}
