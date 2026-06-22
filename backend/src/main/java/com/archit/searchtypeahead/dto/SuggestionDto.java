package com.archit.searchtypeahead.dto;

import lombok.Builder;

@Builder
public record SuggestionDto(
        String term,
        long frequency,
        long recentSearchCount,
        double score
) {
}
