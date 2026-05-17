package com.example.llmwiki.api;

import com.example.llmwiki.graph.EntityAliasService;
import com.example.llmwiki.graph.GraphService;
import com.example.llmwiki.graph.LouvainCommunityDetector;
import com.example.llmwiki.repository.WikiPageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱接口：返回适配 AntV G6 的 nodes/edges 格式。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;
    private final EntityAliasService aliasService;
    private final LouvainCommunityDetector louvain;
    private final WikiPageRepository wikiRepo;

    /**
     * 全量图，可选最低权重过滤。
     */
    @GetMapping
    public Map<String, Object> graph(@RequestParam(value = "minWeight", defaultValue = "0") double minWeight) {
        Map<String, GraphService.NodeInfo> nodes = graphService.nodes();
        Map<String, Map<String, Double>> adj = graphService.adjacency();
        Map<String, Integer> comm = graphService.community();

        List<Map<String, Object>> g6Nodes = new ArrayList<>();
        for (Map.Entry<String, GraphService.NodeInfo> e : nodes.entrySet()) {
            Map<String, Object> n = new HashMap<>();
            n.put("id", e.getKey());
            n.put("label", e.getValue().title);
            n.put("type", e.getValue().type);
            n.put("community", comm.getOrDefault(e.getKey(), -1));
            n.put("degree", graphService.degree(e.getKey()));
            g6Nodes.add(n);
        }

        List<Map<String, Object>> g6Edges = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Map.Entry<String, Map<String, Double>> e : adj.entrySet()) {
            for (Map.Entry<String, Double> n : e.getValue().entrySet()) {
                if (n.getValue() < minWeight) {
                    continue;
                }
                String a = e.getKey();
                String b = n.getKey();
                String key = a.compareTo(b) < 0 ? a + "::" + b : b + "::" + a;
                if (!seen.add(key)) {
                    continue;
                }
                Map<String, Object> ed = new HashMap<>();
                ed.put("source", a);
                ed.put("target", b);
                ed.put("weight", n.getValue());
                g6Edges.add(ed);
            }
        }

        Map<String, Object> r = new HashMap<>();
        r.put("nodes", g6Nodes);
        r.put("edges", g6Edges);
        r.put("communityCount", comm.values().stream().distinct().count());
        return r;
    }

    @GetMapping("/insights")
    public Map<String, Object> insights() {
        Map<String, Object> r = new HashMap<>();
        r.put("isolated", graphService.isolatedNodes());
        r.put("bridges", graphService.bridgeNodes());
        r.put("totalNodes", graphService.nodes().size());
        r.put("totalEdges", graphService.totalEdges());
        return r;
    }

    /**
     * 重建图谱：清空别名表与内存图，用当前 Wiki 页重跑归一化与 upsert。
     * 适用于修改了归一化规则后、或东西已入库但图谱看起来碎裂的场景。
     */
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() {
        try {
            aliasService.clear();
            graphService.clearAll();

            java.util.Map<String, String> remap = new HashMap<>();
            var allPages = wikiRepo.findAll();
            log.info("重建图谱：待处理页面数={}", allPages.size());
            for (var p : allPages) {
                if (p == null || p.getSlug() == null) {
                    continue;
                }
                String canonical = aliasService.canonicalize(
                        p.getType(), p.getTitle() != null ? p.getTitle() : p.getSlug(), p.getSlug());
                remap.put(p.getSlug(), canonical);
            }
            int upserted = 0;
            for (var p : allPages) {
                if (p == null || p.getSlug() == null) {
                    continue;
                }
                String canonicalSlug = remap.getOrDefault(p.getSlug(), p.getSlug());
                com.example.llmwiki.domain.WikiPage proxy = com.example.llmwiki.domain.WikiPage.builder()
                        .id(p.getId())
                        .slug(canonicalSlug)
                        .title(p.getTitle() != null ? p.getTitle() : canonicalSlug)
                        .type(p.getType() != null ? p.getType() : "entity")
                        .build();
                java.util.List<String> outLinks = new java.util.ArrayList<>();
                for (String l : csvToList(p.getOutLinks())) {
                    String mapped = remap.getOrDefault(l, l);
                    if (mapped != null && !mapped.isBlank() && !outLinks.contains(mapped)) {
                        outLinks.add(mapped);
                    }
                }
                java.util.List<String> sources = new java.util.ArrayList<>(csvToList(p.getSources()));
                java.util.List<String> tags = new java.util.ArrayList<>(csvToList(p.getTags()));
                graphService.upsertPage(proxy, outLinks, sources, tags);
                upserted++;
            }
            louvain.detectAndAssign(graphService);
            graphService.persist();
            aliasService.persist();

            Map<String, Object> r = new HashMap<>();
            r.put("nodes", graphService.nodes().size());
            r.put("edges", graphService.totalEdges());
            r.put("upserted", upserted);
            r.put("merged", remap.entrySet().stream().filter(e -> !e.getKey().equals(e.getValue())).count());
            log.info("重建图谱完成: {}", r);
            return r;
        } catch (Exception ex) {
            log.error("重建图谱失败", ex);
            Map<String, Object> r = new HashMap<>();
            r.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            r.put("trace", java.util.Arrays.stream(ex.getStackTrace()).limit(8)
                    .map(StackTraceElement::toString).toList());
            throw new RuntimeException(r.toString(), ex);
        }
    }

    private static java.util.List<String> csvToList(String csv) {
        if (csv == null || csv.isBlank()) { return java.util.List.of(); }
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
