package com.flamingo.qa.extensions;

import com.flamingo.qa.config.Config;
import com.flamingo.qa.util.CustomLogger;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Writes the {@code environment.properties} that Allure renders in its Environment widget.
 * <p>
 * Registered through {@code META-INF/services} rather than {@code @ExtendWith} on purpose: a
 * {@link LauncherSessionListener} runs exactly once per launcher session, before any test class
 * is discovered, so the file is written once regardless of how many suites run or how many
 * threads they run on. Doing it from a {@code @BeforeAll} would mean either duplicating the
 * call in every base class or racing four parallel threads to the same file.
 * <p>
 * The values are read back through {@link Config}, so what the report shows is the
 * <em>effective</em> configuration after any {@code -D} or environment override - not the
 * committed defaults.
 */
public class AllureEnvironmentListener implements LauncherSessionListener {

    private static final CustomLogger log = CustomLogger.getLogger(AllureEnvironmentListener.class);

    private static final Path RESULTS_DIR = Path.of("target", "allure-results");

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        Properties environment = new Properties();
        environment.setProperty("API base URL", Config.apiBaseUrl());
        environment.setProperty("GraphQL URL", Config.graphqlUrl());
        environment.setProperty("UI base URL", Config.uiBaseUrl());
        environment.setProperty("Browser", "Chromium " + (Config.headless() ? "(headless)" : "(headed)"));
        environment.setProperty("UI timeout", Config.uiTimeout().toMillis() + " ms");
        environment.setProperty("API timeout", Config.apiTimeout().toMillis() + " ms");
        environment.setProperty("Java", System.getProperty("java.version"));
        environment.setProperty("OS", System.getProperty("os.name") + " " + System.getProperty("os.version"));

        write(environment);
    }

    private static void write(Properties environment) {
        try {
            Files.createDirectories(RESULTS_DIR);
            try (Writer writer = Files.newBufferedWriter(RESULTS_DIR.resolve("environment.properties"))) {
                environment.store(writer, "Effective configuration for this run");
            }
        } catch (IOException e) {
            log.warn("Could not write the Allure environment file: {}", e.toString());
        }
    }
}
