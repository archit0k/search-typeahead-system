package com.archit.searchtypeahead.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.archit.searchtypeahead.entity.SearchTerm;

public interface SearchTermRepository
        extends JpaRepository<SearchTerm, Long> {

    Optional<SearchTerm> findByTerm(String term);

    List<SearchTerm> findByTermIn(Collection<String> terms);

    List<SearchTerm> findByTermStartingWithIgnoreCaseOrderByFrequencyDesc(
            String prefix,
            Pageable pageable
    );

    List<SearchTerm> findAllByOrderByFrequencyDesc(Pageable pageable);
}
