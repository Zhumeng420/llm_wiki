package com.example.llmwiki.api;

import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.repository.EvalReportRepository;
import com.example.llmwiki.repository.IngestTaskRepository;
import com.example.llmwiki.repository.SourceRecordRepository;
import com.example.llmwiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 总览：把分散统计聚合到一个接口，便于前端首屏快速展现。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final WikiPageRepository wikiRepo;
    private final SourceRecordRepository sourceRepo;
    private final IngestTaskRepository taskRepo;
    private final EvalReportRepository reportRepo;
    private final GraphService graphService;

    @GetMapping
    public Map<String, Object> overview() {
        Map<String, Object> r = new HashMap<>();
        r.put("wikiTotal", wikiRepo.count());
        r.put("sourceTotal", sourceRepo.count());
        r.put("taskTotal", taskRepo.count());
        r.put("reportTotal", reportRepo.count());
        r.put("graphNodes", graphService.nodes().size());
        r.put("graphEdges", graphService.totalEdges());
        r.put("isolated", graphService.isolatedNodes().size());
        r.put("communities", graphService.community().values().stream().distinct().count());
        r.put("recentTasks", taskRepo.findTop50ByOrderByIdDesc().stream().limit(10).toList());
        return r;
    }
}
