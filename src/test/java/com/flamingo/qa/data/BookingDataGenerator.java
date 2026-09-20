package com.flamingo.qa.data;

import com.flamingo.qa.pojo.booking.Booking;
import com.flamingo.qa.pojo.booking.BookingDates;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds booking data that is unique per run. Uniqueness is the point, not realism:
 * Restful Booker is shared, and {@code GET /booking?firstname=} searches across everyone's
 * data, so a booking for "John Smith" would match strangers' records.
 */
public final class BookingDataGenerator {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private BookingDataGenerator() {
    }

    public static Booking validBooking() {
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

    public static Booking bookingStaying(LocalDate checkIn, LocalDate checkOut) {
        return validBooking().toBuilder()
                .bookingDates(BookingDates.builder().checkin(checkIn).checkout(checkOut).build())
                .build();
    }

    public static String uniqueLastName() {
        return FAKER.name().lastName() + "-" + FAKER.internet().uuid().substring(0, 8);
    }
}
