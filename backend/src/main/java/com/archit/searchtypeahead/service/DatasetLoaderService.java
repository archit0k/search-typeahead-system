package com.archit.searchtypeahead.service;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.archit.searchtypeahead.entity.SearchTerm;
import com.archit.searchtypeahead.repository.SearchTermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DatasetLoaderService {

    private final SearchTermRepository repository;

    @Value("${app.dataset.load-on-startup:true}")
    private boolean loadOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void loadDataset() {

        if (!loadOnStartup) {
            System.out.println("Dataset loading skipped for this profile");
            return;
        }

        if (repository.count() > 0) {
            System.out.println("Dataset already loaded");
            return;
        }

        Path filePath = Path.of("../dataset/count_1w.txt").normalize();

        List<SearchTerm> batch = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {

            String line;
            int batchSize = 1000;
            long totalLoaded = 0;

            while ((line = reader.readLine()) != null) {

                int separatorIndex = line.lastIndexOf(' ');

                if (separatorIndex <= 0) {
                    continue;
                }

                String term = normalize(line.substring(0, separatorIndex));

                long frequency;

                try {
                    frequency = Long.parseLong(line.substring(separatorIndex + 1).trim());
                } catch (Exception e) {
                    continue;
                }

                batch.add(
                        SearchTerm.builder()
                                .term(term)
                                .frequency(frequency)
                                .lastSearchedAt(LocalDateTime.now())
                                .build());

                if (batch.size() >= batchSize) {

                    repository.saveAll(batch);

                    totalLoaded += batch.size();

                    if (totalLoaded % 10000 == 0) {
                        System.out.println(
                                "Loaded rows: " + totalLoaded);
                    }

                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {

                repository.saveAll(batch);

                totalLoaded += batch.size();
            }

            System.out.println(
                    "Dataset load complete. Total rows: "
                            + totalLoaded);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalize(String term) {
        return term.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
