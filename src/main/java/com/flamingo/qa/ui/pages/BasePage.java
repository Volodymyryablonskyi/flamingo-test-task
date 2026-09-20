package com.flamingo.qa.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * What every page object shares: the page handle and how to open itself.
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
