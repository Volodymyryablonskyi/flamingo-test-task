package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class Character {

    String id;
    String name;
    String status;
    String species;
    String gender;
    List<Episode> episode;
}
