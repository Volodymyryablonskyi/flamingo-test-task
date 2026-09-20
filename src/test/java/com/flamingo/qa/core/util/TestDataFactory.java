package com.flamingo.qa.core.util;

import com.flamingo.qa.api.model.booking.Booking;
import com.flamingo.qa.api.model.booking.BookingDates;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds realistic, unique test data.
 *
 * <p>Uniqueness is the point, not realism. Restful Booker is a shared public service that
 * other people are hitting at the same time, and {@code GET /booking?firstname=…} searches
 * across everyone's data - so a booking for "John Smith" would match strangers' records and
 * make a search assertion meaningless. Every generated surname carries a run-unique suffix.
 *
 * <p>{@link Faker} is thread-safe for the generators used here, and test classes run
 * concurrently, so one shared instance is fine.
 */
public final class TestDataFactory {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private TestDataFactory() {
    }

    /** A complete, valid booking with every field populated and a globally unique guest. */
    public static Booking aBooking() {
        LocalDate checkIn = LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 90));
        return Booking.builder()
                .firstname(FAKER.name().firstName())
                .lastname(uniqueLastName())
                .totalPrice(ThreadLocalRandom.current().nextInt(50, 5_000))
                .depositPaid(FAKER.bool().bool())
                .bookingDates(BookingDates.builder()
                        .checkin(checkIn)
                        .checkout(checkIn.plusDays(ThreadLocalRandom.current().nextInt(1, 21)))
                        .build())
                .additionalNeeds(FAKER.options().option("Breakfast", "Late checkout", "Extra pillows"))
                .build();
    }

    /**
     * A surname no other client of this shared service will be using, so searches and
     * round-trip assertions are about our own data and nobody else's.
     */
    public static String uniqueLastName() {
        return FAKER.name().lastName() + "-" + FAKER.internet().uuid().substring(0, 8);
    }
}
