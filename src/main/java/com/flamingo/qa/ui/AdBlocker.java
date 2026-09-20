package com.flamingo.qa.ui;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Route;

import java.util.List;

/**
 * Suppresses the adverts DemoQA serves.
 *
 * <p>This is the site's signature flake: ad iframes load late, reflow the page and swallow
 * clicks aimed at whatever was underneath, and a sticky banner covers the submit button.
 * Blocking the requests outright is more reliable than any amount of scrolling or retrying,
 * and it makes runs faster and quieter.
 *
 * <p>Applied centrally when a context is created, so no test or page object repeats it.
 */
public final class AdBlocker {

    private static final List<String> AD_HOSTS = List.of(
            "googlesyndication.com",
            "doubleclick.net",
            "googletagservices.com",
            "google-analytics.com",
            "googletagmanager.com",
            "adservice.google.com",
            "pagead2.googlesyndication.com");

    /** The fixed banner and footer sit over page content even when no ad loads. */
    private static final String HIDE_OVERLAYS = """
            document.addEventListener('DOMContentLoaded', () => {
                const style = document.createElement('style');
                style.textContent = '#fixedban, footer, #adplus-anchor, .adsbygoogle { display: none !important; }';
                document.head.appendChild(style);
            });""";

    private AdBlocker() {
    }

    public static void applyTo(BrowserContext context) {
        context.route(AdBlocker::isAdvert, Route::abort);
        context.addInitScript(HIDE_OVERLAYS);
    }

    private static boolean isAdvert(String url) {
        return AD_HOSTS.stream().anyMatch(url::contains);
    }
}
