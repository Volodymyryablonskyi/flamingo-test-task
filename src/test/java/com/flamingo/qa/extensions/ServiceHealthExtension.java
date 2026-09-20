package com.flamingo.qa.extensions;

import com.flamingo.qa.config.Config;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceHealthExtension implements BeforeAllCallback {

    private static final Map<SystemUnderTest, Optional<String>> PROBE_RESULTS = new ConcurrentHashMap<>();

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!Config.healthCheckEnabled()) {
            return;
        }
        for (SystemUnderTest system : requiredSystems(context)) {
            PROBE_RESULTS.computeIfAbsent(system, ServiceHealthExtension::probe)
                    .ifPresent(Assumptions::abort);
        }
    }

    private static SystemUnderTest[] requiredSystems(ExtensionContext context) {
        return context.getTestClass()
                .flatMap(testClass -> AnnotationSupport.findAnnotation(testClass, RequiresService.class))
                .map(RequiresService::value)
                .orElse(new SystemUnderTest[0]);
    }

    private static Optional<String> probe(SystemUnderTest system) {
        String url = system.url();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Config.healthCheckTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        try {
            HttpResponse<Void> response = client.send(request(system, url),
                    HttpResponse.BodyHandlers.discarding());

            if (system.isHealthyStatus(response.statusCode())) {
                return Optional.empty();
            }
            return Optional.of(system.displayName() + " health check on " + url
                    + " returned HTTP " + response.statusCode() + " - skipping rather than"
                    + " reporting someone else's outage as a failure.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of(system.displayName() + " health check was interrupted.");
        } catch (Exception e) {
            return Optional.of(system.displayName() + " is not reachable at " + url
                    + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")"
                    + " - skipping rather than reporting someone else's outage as a failure.");
        }
    }

    private static HttpRequest request(SystemUnderTest system, String url) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Config.healthCheckTimeout());
        String body = system.postBody();
        return body == null
                ? builder.GET().build()
                : builder.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
    }
}
