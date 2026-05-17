package com.example.llmwiki.repository;

import com.example.llmwiki.domain.EvalReport;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author llm-wiki
 * @since 1.0.0
 */
public interface EvalReportRepository extends JpaRepository<EvalReport, Long> {
}
