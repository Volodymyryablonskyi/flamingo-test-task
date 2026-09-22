package com.flamingo.qa.ui.components;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class SubmissionModal extends BaseComponent {

    private final Locator container = page.locator(".modal-content");
    private final Locator title = page.locator("#example-modal-sizes-title-lg");
    private final Locator rows = page.locator(".modal-body tbody tr");

    public SubmissionModal(Page page) {
        super(page);
    }

    public Map<String, String> submittedValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Locator row : rows.all()) {
            Locator cells = row.locator("td");
            values.put(cells.first().innerText().trim(), cells.nth(1).innerText().trim());
        }
        return values;
    }
}
