package com.flamingo.qa.pojo.ui;

import lombok.Builder;

import java.util.List;

@Builder(toBuilder = true)
public record Employee(String firstName,
                       String lastName,
                       String email,
                       String age,
                       String salary,
                       String department) {

    public List<String> asRow() {
        return List.of(firstName, lastName, age, email, salary, department);
    }
}
