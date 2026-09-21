package com.flamingo.qa.data;

import com.flamingo.qa.pojo.ui.Employee;
import com.flamingo.qa.pojo.ui.Student;
import net.datafaker.Faker;

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class StudentDataGenerator {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private StudentDataGenerator() {
    }

    public static Student completeStudent() {
        return Student.builder()
                .firstName(FAKER.name().firstName())
                .lastName(FAKER.name().lastName())
                .email(FAKER.internet().emailAddress())
                .gender(FAKER.options().option("Male", "Female", "Other"))
                .mobile(String.valueOf(ThreadLocalRandom.current().nextLong(6_000_000_000L, 9_999_999_999L)))
                .dateOfBirth(LocalDate.of(1995, 6, 15))
                .subject("Maths")
                .hobby(FAKER.options().option("Sports", "Reading", "Music"))
                .pictureName("sample-upload.png")
                .currentAddress(FAKER.address().streetAddress())
                .state("NCR")
                .city("Delhi")
                .build();
    }

    public static Employee employee() {
        return Employee.builder()
                .firstName(FAKER.name().firstName())
                .lastName(FAKER.name().lastName() + ThreadLocalRandom.current().nextInt(1_000, 9_999))
                .email(FAKER.internet().emailAddress())
                .age(String.valueOf(ThreadLocalRandom.current().nextInt(21, 65)))
                .salary(String.valueOf(ThreadLocalRandom.current().nextInt(1_000, 99_999)))
                .department(FAKER.options().option("Engineering", "Legal", "Compliance", "Insurance"))
                .build();
    }
}
