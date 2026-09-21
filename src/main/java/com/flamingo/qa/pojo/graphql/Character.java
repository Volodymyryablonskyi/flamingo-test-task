package com.flamingo.qa.pojo.graphql;

import java.util.List;

public record Character(String id,
                        String name,
                        String status,
                        String species,
                        String gender,
                        List<Episode> episode) {
}
