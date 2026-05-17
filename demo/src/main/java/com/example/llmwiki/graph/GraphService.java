package com.example.llmwiki.graph;

import com.example.llmwiki.config.StorageProperties;
import com.example.llmwiki.domain.WikiPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 图谱服务（内存图 + JSON 持久化）。
 * <p>
 * 维护节点元信息、邻接表与社区划分；提供 4 信号 relevance 计算、Louvain 分簇、
 * 以及孤立节点 / 稀疏社区 / 桥节点等结构性洞察。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** slug -> NodeInfo */
    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();
    /** slug -> 邻居 slug -> 权重 */
    private final Map<String, Map<String, Double>> adjacency = new ConcurrentHashMap<>();
    /** slug -> communityId */
    private final Map<String, Integer> community = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        File f = graphFile();
        if (f.exists()) {
            try {
                Snapshot s = objectMapper.readValue(f, Snapshot.class);
                if (s.nodes != null) {
                    nodes.putAll(s.nodes);
                }
                if (s.adjacency != null) {
                    adjacency.putAll(s.adjacency);
                }
                if (s.community != null) {
                    community.putAll(s.community);
                }
                log.info("加载图谱: nodes={} edges={}", nodes.size(), totalEdges());
            } catch (Exception e) {
                log.warn("加载图谱失败: {}", e.getMessage());
            }
        }
    }

    public synchronized void upsertPage(WikiPage page, List<String> outLinks, List<String> sources, List<String> tags) {
        NodeInfo info = nodes.computeIfAbsent(page.getSlug(), k -> new NodeInfo());
        info.slug = page.getSlug();
        info.title = page.getTitle();
        info.type = page.getType();
        info.sources = sources == null ? List.of() : sources;
        info.tags = tags == null ? List.of() : tags;
        // 重建本节点的出边（保留入边由对端维护）
        Map<String, Double> out = adjacency.computeIfAbsent(page.getSlug(), k -> new HashMap<>());
        out.clear();
        if (outLinks != null) {
            for (String l : outLinks) {
                if (l == null || l.isBlank() || l.equals(page.getSlug())) {
                    continue;
                }
                out.merge(l, 3.0, Double::sum); // direct link weight = 3.0
                adjacency.computeIfAbsent(l, k -> new HashMap<>()).merge(page.getSlug(), 3.0, Double::sum);
            }
        }
        // source overlap强连接
        if (sources != null && !sources.isEmpty()) {
            for (NodeInfo other : new HashMap<>(nodes).values()) {
                if (other.slug.equals(page.getSlug())) {
                    continue;
                }
                long overlap = other.sources == null ? 0 :
                        other.sources.stream().filter(sources::contains).count();
                if (overlap > 0) {
                    double w = 4.0 * overlap;
                    adjacency.computeIfAbsent(page.getSlug(), k -> new HashMap<>()).merge(other.slug, w, Double::max);
                    adjacency.computeIfAbsent(other.slug, k -> new HashMap<>()).merge(page.getSlug(), w, Double::max);
                }
            }
        }
        // tag overlap弱连接（跨源联通的关键：同标签的两个页面加 1.5*重叠数的边）
        if (tags != null && !tags.isEmpty()) {
            for (NodeInfo other : new HashMap<>(nodes).values()) {
                if (other.slug.equals(page.getSlug())) {
                    continue;
                }
                if (other.tags == null || other.tags.isEmpty()) {
                    continue;
                }
                long overlap = other.tags.stream().filter(tags::contains).count();
                if (overlap > 0) {
                    double w = 1.5 * overlap;
                    adjacency.computeIfAbsent(page.getSlug(), k -> new HashMap<>()).merge(other.slug, w, Double::max);
                    adjacency.computeIfAbsent(other.slug, k -> new HashMap<>()).merge(page.getSlug(), w, Double::max);
                }
            }
        }
    }

    /** 保留旧签名以兼容（如后续有调用未迁移的代码）。 */
    public void upsertPage(WikiPage page, List<String> outLinks, List<String> sources) {
        upsertPage(page, outLinks, sources, List.of());
    }

    public synchronized void persist() {
        File f = graphFile();
        try {
            f.getParentFile().mkdirs();
            Snapshot s = new Snapshot();
            s.nodes = nodes;
            s.adjacency = adjacency;
            s.community = community;
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, s);
        } catch (Exception e) {
            log.warn("持久化图谱失败: {}", e.getMessage());
        }
    }

    public Map<String, NodeInfo> nodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public Map<String, Map<String, Double>> adjacency() {
        return Collections.unmodifiableMap(adjacency);
    }

    public int totalEdges() {
        int c = 0;
        for (Map<String, Double> m : adjacency.values()) {
            c += m.size();
        }
        return c / 2;
    }

    public Set<String> neighbors(String slug) {
        return adjacency.getOrDefault(slug, Map.of()).keySet();
    }

    public int degree(String slug) {
        return neighbors(slug).size();
    }

    public List<String> isolatedNodes() {
        return nodes.keySet().stream().filter(s -> degree(s) <= 1).sorted().collect(Collectors.toList());
    }

    /**
     * 找出连接 >=3 个不同社区的桥节点。
     */
    public List<String> bridgeNodes() {
        if (community.isEmpty()) {
            return List.of();
        }
        return nodes.keySet().stream()
                .filter(s -> {
                    Set<Integer> comms = new HashSet<>();
                    for (String n : neighbors(s)) {
                        Integer c = community.get(n);
                        if (c != null) {
                            comms.add(c);
                        }
                    }
                    return comms.size() >= 3;
                })
                .sorted().collect(Collectors.toList());
    }

    public void setCommunity(Map<String, Integer> map) {
        community.clear();
        community.putAll(map);
    }

    /** 清空内存图谱（不删除文件）。 */
    public synchronized void clearAll() {
        nodes.clear();
        adjacency.clear();
        community.clear();
    }

    public Map<String, Integer> community() {
        return Collections.unmodifiableMap(community);
    }

    private File graphFile() {
        return new File(storageProperties.getGraphDir(), "graph.json");
    }

    @Data
    public static class NodeInfo {
        public String slug;
        public String title;
        public String type;
        public List<String> sources;
        public List<String> tags;
    }

    @Data
    public static class Snapshot {
        public Map<String, NodeInfo> nodes;
        public Map<String, Map<String, Double>> adjacency;
        public Map<String, Integer> community;
    }
}
