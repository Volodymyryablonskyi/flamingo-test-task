package com.flamingo.qa.endpoints;

/** Paths live here, not scattered through the client, so a moved endpoint is one edit. */
public class BookingEndpoints implements Endpoints {

    private static final String BOOKING = "/booking";
    private static final String BOOKING_BY_ID = BOOKING + "/%d";

    public String getCreateUri() {
        return BOOKING;
    }

    public String getSearchUri() {
        return BOOKING;
    }

    public String getByIdUri(int bookingId) {
        return String.format(BOOKING_BY_ID, bookingId);
    }
}
