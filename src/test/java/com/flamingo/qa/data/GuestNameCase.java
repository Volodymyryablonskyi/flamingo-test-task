package com.flamingo.qa.data;

/**
 * One row of {@code testdata/guest-name-cases.json}. The names are chosen to exercise the
 * encoding of the search query string - apostrophes and accents are where a name-based
 * lookup usually breaks.
 */
public record GuestNameCase(String description, String firstname, String lastname) {

    @Override
    public String toString() {
        return description + " (" + firstname + " " + lastname + ")";
    }
}
