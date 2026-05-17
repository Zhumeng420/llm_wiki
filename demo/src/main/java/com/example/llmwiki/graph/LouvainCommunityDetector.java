package com.example.llmwiki.graph;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 简化版 Louvain 社区发现：单层贪心模块度优化。
 * <p>
 * 算法思路：每个节点初始为独立社区，循环把节点移动到能使模块度提升最大的邻居社区，
 * 直到无法继续改进。复杂度对个人 wiki 规模（节点 < 5k）足够。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LouvainCommunityDetector {

    /**
     * 运行社区发现并把结果写回 GraphService。
     *
     * @return communityId -> 节点列表
     */
    public Map<Integer, List<String>> detectAndAssign(GraphService graphService) {
        Map<String, Map<String, Double>> adj = graphService.adjacency();
        // 同时纳入 nodes 与 adjacency 中的所有 slug，避免“悬空边”导致邻居 lookup 为 null
        Set<String> nodes = new HashSet<>(graphService.nodes().keySet());
        for (Map.Entry<String, Map<String, Double>> e : adj.entrySet()) {
            nodes.add(e.getKey());
            if (e.getValue() != null) {
                nodes.addAll(e.getValue().keySet());
            }
        }
        if (nodes.isEmpty()) {
            graphService.setCommunity(Map.of());
            return Map.of();
        }

        // 初始：每个节点一个 community
        Map<String, Integer> comm = new HashMap<>();
        int idx = 0;
        for (String n : nodes) {
            comm.put(n, idx++);
        }

        double m2 = 0; // 2m
        Map<String, Double> nodeStrength = new HashMap<>();
        for (String n : nodes) {
            double s = adj.getOrDefault(n, Map.of()).values().stream().mapToDouble(Double::doubleValue).sum();
            nodeStrength.put(n, s);
            m2 += s;
        }
        if (m2 == 0) {
            graphService.setCommunity(comm);
            return groupBy(comm);
        }

        boolean improved = true;
        int iter = 0;
        while (improved && iter < 30) {
            improved = false;
            iter++;
            for (String n : nodes) {
                int curC = comm.get(n);
                Map<Integer, Double> gainPerComm = new HashMap<>();
                Map<String, Double> nbrs = adj.getOrDefault(n, Map.of());
                double ki = nodeStrength.get(n);
                // 计算到各邻居社区的连边权重和
                Map<Integer, Double> sumIn = new HashMap<>();
                for (Map.Entry<String, Double> e : nbrs.entrySet()) {
                    Integer c2 = comm.get(e.getKey());
                    if (c2 == null) {
                        continue;
                    }
                    sumIn.merge(c2, e.getValue(), Double::sum);
                }
                // 估算社区内总度
                Map<Integer, Double> sumTot = new HashMap<>();
                for (String x : nodes) {
                    sumTot.merge(comm.get(x), nodeStrength.get(x), Double::sum);
                }
                int bestC = curC;
                double bestGain = 0;
                for (Map.Entry<Integer, Double> e : sumIn.entrySet()) {
                    int c2 = e.getKey();
                    double sIn = e.getValue();
                    double sTot = sumTot.getOrDefault(c2, 0.0) - (c2 == curC ? ki : 0);
                    double dQ = sIn - sTot * ki / m2;
                    if (dQ > bestGain + 1e-9) {
                        bestGain = dQ;
                        bestC = c2;
                    }
                }
                if (bestC != curC) {
                    comm.put(n, bestC);
                    improved = true;
                }
            }
        }

        // 压缩社区编号
        Map<Integer, Integer> remap = new HashMap<>();
        int next = 0;
        Map<String, Integer> finalComm = new HashMap<>();
        for (Map.Entry<String, Integer> e : comm.entrySet()) {
            int c = remap.computeIfAbsent(e.getValue(), k -> remap.size());
            finalComm.put(e.getKey(), c);
            next = Math.max(next, c);
        }
        graphService.setCommunity(finalComm);
        log.info("Louvain 完成: 社区数={} iter={}", next + 1, iter);
        return groupBy(finalComm);
    }

    /**
     * 计算社区内聚度：actualEdges / possibleEdges（无向图）。
     */
    public double cohesion(GraphService graph, List<String> members) {
        if (members.size() < 2) {
            return 0;
        }
        Set<String> set = new HashSet<>(members);
        long edges = 0;
        for (String s : members) {
            for (String t : graph.neighbors(s)) {
                if (set.contains(t) && s.compareTo(t) < 0) {
                    edges++;
                }
            }
        }
        long possible = (long) members.size() * (members.size() - 1) / 2;
        return possible == 0 ? 0 : (double) edges / possible;
    }

    private Map<Integer, List<String>> groupBy(Map<String, Integer> comm) {
        Map<Integer, List<String>> g = new HashMap<>();
        for (Map.Entry<String, Integer> e : comm.entrySet()) {
            g.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
        }
        return g;
    }
}
