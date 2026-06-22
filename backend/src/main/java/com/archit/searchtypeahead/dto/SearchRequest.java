package com.archit.searchtypeahead.dto;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "term is required")
        String term
) {
}
