package com.flamingo.qa.ui;

import com.flamingo.qa.config.Config;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.flamingo.qa.util.CustomLogger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the expensive, reusable half of the browser stack.
 *
 * <p>{@link Playwright} and {@link Browser} are created once per <em>thread</em> and kept:
 * launching a browser costs seconds, and test classes run in parallel on a fixed pool, so
 * per-thread reuse gives each worker its own browser without paying the cost per test.
 * A {@link BrowserContext} is created per <em>test</em> instead - it is cheap, and a fresh
 * one guarantees no cookie or storage leaks between tests, which is what makes parallel
 * execution safe.
 */
public final class BrowserFactory {

    private static final CustomLogger log = CustomLogger.getLogger(BrowserFactory.class);

    private static final ThreadLocal<Playwright> PLAYWRIGHT = ThreadLocal.withInitial(BrowserFactory::createPlaywright);
    private static final ThreadLocal<Browser> BROWSER = ThreadLocal.withInitial(BrowserFactory::launchBrowser);

    /** Every instance created on any thread, so the run can shut all of them down. */
    private static final Set<Playwright> CREATED = ConcurrentHashMap.newKeySet();

    private BrowserFactory() {
    }

    public static BrowserContext newContext() {
        BrowserContext context = BROWSER.get().newContext(new Browser.NewContextOptions()
                .setBaseURL(Config.uiBaseUrl())
                .setViewportSize(Config.viewportWidth(), Config.viewportHeight()));
        context.setDefaultTimeout(Config.uiTimeout().toMillis());
        return context;
    }

    /** Closes every browser the run started. Best effort: shutdown must not fail a suite. */
    public static void closeAll() {
        for (Playwright playwright : CREATED) {
            try {
                playwright.close();
            } catch (RuntimeException e) {
                log.warn("Could not close Playwright cleanly: {}", e.toString());
            }
        }
        CREATED.clear();
    }

    private static Playwright createPlaywright() {
        Playwright playwright = Playwright.create();
        CREATED.add(playwright);
        return playwright;
    }

    private static Browser launchBrowser() {
        return PLAYWRIGHT.get().chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Config.headless())
                .setSlowMo(Config.uiSlowMo().toMillis()));
    }
}
