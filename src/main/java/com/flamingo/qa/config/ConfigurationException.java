package com.flamingo.qa.config;

/** Configuration is missing or malformed. Unchecked: there is no recovery from it. */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
