package com.example.llmwiki.repository;

import com.example.llmwiki.domain.SourceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author llm-wiki
 * @since 1.0.0
 */
public interface SourceRecordRepository extends JpaRepository<SourceRecord, Long> {

    Optional<SourceRecord> findByKindAndRef(String kind, String ref);

    List<SourceRecord> findByKindIn(List<String> kinds);

    List<SourceRecord> findByWatchEnabledTrue();
}
