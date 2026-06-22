package com.archit.searchtypeahead.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.archit.searchtypeahead.dto.SuggestionDto;
import com.archit.searchtypeahead.service.SuggestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class SuggestController {

    private final SuggestService suggestService;

    @GetMapping("/suggest")
    public List<SuggestionDto> suggest(
            @RequestParam(required = false) String q
    ) {

        return suggestService.getSuggestions(q);
    }
}
