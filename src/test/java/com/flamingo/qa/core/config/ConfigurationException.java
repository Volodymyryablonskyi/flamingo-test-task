package com.flamingo.qa.core.config;

/**
 * Thrown when configuration is missing or malformed. Unchecked by design: there is no
 * meaningful recovery from a misconfigured suite, and the message always names the key.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
