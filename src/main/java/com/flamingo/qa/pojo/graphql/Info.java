package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class Info {

    Integer count;
    Integer pages;
    Integer next;
    Integer prev;
}
