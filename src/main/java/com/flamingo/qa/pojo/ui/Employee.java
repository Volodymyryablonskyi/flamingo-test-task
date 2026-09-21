package com.flamingo.qa.pojo.ui;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class Employee {

    String firstName;
    String lastName;
    String email;
    String age;
    String salary;
    String department;

    public List<String> asRow() {
        return List.of(firstName, lastName, age, email, salary, department);
    }
}
