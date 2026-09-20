package com.flamingo.qa.extensions;

import com.flamingo.qa.config.Config;
import com.flamingo.qa.ui.BrowserFactory;
import com.flamingo.qa.util.CustomLogger;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlaywrightExtension implements BeforeEachCallback, AfterEachCallback {

    private static final CustomLogger log = CustomLogger.getLogger(PlaywrightExtension.class);

    private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");
    private static final Path TRACE_DIR = Path.of("target", "traces");

    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    public static Page page() {
        Page page = PAGE.get();
        if (page == null) {
            throw new IllegalStateException(
                    "No Playwright page on this thread - is the test class extending BaseUiTest?");
        }
        return page;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        registerRunShutdown(extensionContext);

        BrowserContext browserContext = BrowserFactory.newContext();
        if (Config.traceEnabled()) {
            browserContext.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));
        }

        CONTEXT.set(browserContext);
        PAGE.set(browserContext.newPage());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        BrowserContext browserContext = CONTEXT.get();
        if (browserContext == null) {
            return;
        }
        try {
            boolean failed = extensionContext.getExecutionException().isPresent();
            String name = testName(extensionContext);
            if (failed) {
                captureScreenshot(name);
            }
            stopTracing(browserContext, failed ? TRACE_DIR.resolve(name + ".zip") : null);
        } finally {
            browserContext.close();
            CONTEXT.remove();
            PAGE.remove();
        }
    }

    private void captureScreenshot(String name) {
        try {
            Files.createDirectories(SCREENSHOT_DIR);
            Path file = SCREENSHOT_DIR.resolve(name + ".png");
            byte[] image = PAGE.get().screenshot(new Page.ScreenshotOptions()
                    .setPath(file)
                    .setFullPage(true));
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(image), ".png");
            log.warn("Test failed - screenshot written to {}", file);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not capture a screenshot: {}", e.toString());
        }
    }

    private void stopTracing(BrowserContext browserContext, Path traceFile) {
        if (!Config.traceEnabled()) {
            return;
        }
        try {
            if (traceFile == null) {
                browserContext.tracing().stop();
                return;
            }
            Files.createDirectories(TRACE_DIR);
            browserContext.tracing().stop(new Tracing.StopOptions().setPath(traceFile));
            log.warn("Trace written to {} - open it with: npx playwright show-trace {}",
                    traceFile, traceFile);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not save the Playwright trace: {}", e.toString());
        }
    }

    private static String testName(ExtensionContext extensionContext) {
        return extensionContext.getRequiredTestClass().getSimpleName()
                + "." + extensionContext.getRequiredTestMethod().getName();
    }

    private static void registerRunShutdown(ExtensionContext extensionContext) {
        extensionContext.getRoot()
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent("playwright-shutdown", key -> new BrowserShutdown());
    }

    private record BrowserShutdown() implements AutoCloseable {

        @Override
        public void close() {
            BrowserFactory.closeAll();
        }
    }
}
