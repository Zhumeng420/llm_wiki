package com.example.llmwiki.api;

import com.example.llmwiki.domain.WikiPage;
import com.example.llmwiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Wiki 页面接口：列表 / 按 slug 查看详情 / 类型分组统计。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/wiki")
@RequiredArgsConstructor
public class WikiController {

    private final WikiPageRepository wikiRepo;

    @GetMapping("/pages")
    public List<WikiPage> list(@RequestParam(value = "type", required = false) String type) {
        return type == null || type.isBlank() ? wikiRepo.findAll() : wikiRepo.findByType(type);
    }

    @GetMapping("/pages/{slug}")
    public ResponseEntity<WikiPage> detail(@PathVariable String slug) {
        return wikiRepo.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long total = wikiRepo.count();
        Map<String, Long> byType = new java.util.HashMap<>();
        for (WikiPage p : wikiRepo.findAll()) {
            byType.merge(p.getType() == null ? "unknown" : p.getType(), 1L, Long::sum);
        }
        return Map.of("total", total, "byType", byType);
    }
}
