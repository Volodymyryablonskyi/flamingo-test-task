package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * Only the fields the queries actually select. A GraphQL response contains exactly what was
 * asked for, so a field absent from the document arrives null - which is correct here and
 * not a mapping failure.
 */
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
