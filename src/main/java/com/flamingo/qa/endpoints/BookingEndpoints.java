package com.flamingo.qa.endpoints;

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
