package com.flamingo.qa.data;

public record GuestNameCase(String description, String firstname, String lastname) {

    @Override
    public String toString() {
        return description + " (" + firstname + " " + lastname + ")";
    }
}
