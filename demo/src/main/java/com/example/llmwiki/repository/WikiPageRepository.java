package com.example.llmwiki.repository;

import com.example.llmwiki.domain.WikiPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author llm-wiki
 * @since 1.0.0
 */
public interface WikiPageRepository extends JpaRepository<WikiPage, Long> {

    Optional<WikiPage> findBySlug(String slug);

    List<WikiPage> findByType(String type);
}
