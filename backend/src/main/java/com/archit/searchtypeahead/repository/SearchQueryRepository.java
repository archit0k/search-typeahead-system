package com.archit.searchtypeahead.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.archit.searchtypeahead.entity.SearchQuery;

public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {

    Optional<SearchQuery> findByQuery(String query);

}