package com.archit.searchtypeahead.dto;

import lombok.Builder;

@Builder
public record SearchResponse(
        String message,
        String term
) {
}
