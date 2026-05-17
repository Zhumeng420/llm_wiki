package com.example.llmwiki.repository;

import com.example.llmwiki.domain.IngestTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author llm-wiki
 * @since 1.0.0
 */
public interface IngestTaskRepository extends JpaRepository<IngestTask, Long> {

    List<IngestTask> findTop50ByOrderByIdDesc();

    List<IngestTask> findByStatusOrderByIdAsc(String status);
}
