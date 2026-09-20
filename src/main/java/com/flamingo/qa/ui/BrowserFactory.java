package com.flamingo.qa.ui;

import com.flamingo.qa.config.Config;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.flamingo.qa.util.CustomLogger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BrowserFactory {

    private static final CustomLogger log = CustomLogger.getLogger(BrowserFactory.class);

    private static final ThreadLocal<Playwright> PLAYWRIGHT = ThreadLocal.withInitial(BrowserFactory::createPlaywright);
    private static final ThreadLocal<Browser> BROWSER = ThreadLocal.withInitial(BrowserFactory::launchBrowser);

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
