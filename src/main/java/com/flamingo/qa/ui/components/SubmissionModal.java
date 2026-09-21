package com.flamingo.qa.ui.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class SubmissionModal {

    private final Page page;
    private final Locator container;
    private final Locator title;
    private final Locator rows;

    public SubmissionModal(Page page) {
        this.page = page;
        this.container = page.locator(".modal-content");
        this.title = page.locator("#example-modal-sizes-title-lg");
        this.rows = page.locator(".modal-body tbody tr");
    }

    public Map<String, String> submittedValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Locator row : rows.all()) {
            Locator cells = row.locator("td");
            values.put(cells.nth(0).innerText().trim(), cells.nth(1).innerText().trim());
        }
        return values;
    }
}
