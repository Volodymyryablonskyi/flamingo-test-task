package com.flamingo.qa.ui.components;

import com.microsoft.playwright.Page;

public abstract class BaseComponent {

    protected final Page page;

    protected BaseComponent(Page page) {
        this.page = page;
    }
}
