package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/** Pagination metadata; {@code next} and {@code prev} are null at the ends of the range. */
@Value
@Builder
@Jacksonized
public class Info {

    Integer count;
    Integer pages;
    Integer next;
    Integer prev;
}
