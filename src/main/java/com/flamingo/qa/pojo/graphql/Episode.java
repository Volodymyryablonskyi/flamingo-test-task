package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class Episode {

    String id;
    String name;
    /** The production code, e.g. {@code S01E01} - the schema names this field "episode". */
    String episode;
}
