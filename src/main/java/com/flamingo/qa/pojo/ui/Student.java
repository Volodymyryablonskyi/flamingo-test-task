package com.flamingo.qa.pojo.ui;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder(toBuilder = true)
public class Student {

    String firstName;
    String lastName;
    String email;
    String gender;
    String mobile;
    LocalDate dateOfBirth;
    String subject;
    String hobby;
    String pictureName;
    String currentAddress;
    String state;
    String city;

    public String fullName() {
        return firstName + " " + lastName;
    }
}
