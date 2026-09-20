package com.flamingo.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * What every page object shares: the page handle and how to open itself.
 *
 * <p><strong>Convention for every page object:</strong> locators are {@code private final}
 * fields exposed through Lombok {@code @Getter}, never methods that build one per call.
 * A Playwright {@link com.microsoft.playwright.Locator} is a lazy description resolved on
 * each action, so a field cannot go stale, and the page then reads as a declaration of what
 * is on the screen rather than a bag of factory methods.
 *
 * <p>No waiting helpers here on purpose. Playwright's locators auto-wait for an element to
 * be attached, visible and stable before acting, and {@code PlaywrightAssertions} retry
 * until timeout, so a hand-rolled wait layer would only be a slower, buggier copy of what
 * the library already guarantees. {@code Thread.sleep} is banned in this package.
 */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    /** Path relative to {@code ui.base.url}, which the browser context carries as its base URL. */
    protected abstract String path();

    public void open() {
        page.navigate(path());
    }

    /**
     * DemoQA's sticky footer overlaps controls at the bottom of long forms; Playwright
     * scrolls before acting anyway, but doing it explicitly keeps the element off the
     * banner's edge where a click can still land on the wrong thing.
     */
    protected void scrollIntoView(Locator locator) {
        locator.scrollIntoViewIfNeeded();
    }
}
