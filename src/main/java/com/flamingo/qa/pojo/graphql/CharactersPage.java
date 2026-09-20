package com.flamingo.qa.pojo.graphql;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class CharactersPage {

    Info info;
    List<Character> results;
}
