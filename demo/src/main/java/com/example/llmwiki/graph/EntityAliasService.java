package com.example.llmwiki.graph;

import com.example.llmwiki.config.StorageProperties;
import com.example.llmwiki.util.TextUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 实体别名归一化服务。
 * <p>
 * 解决 LLM 在不同源里把同一实体写成不同 slug（如「阿里云函数计算FC」/「阿里云函数计算」/「函数计算 FC」），
 * 导致图谱出现多个互不连通子图的问题。
 * </p>
 * 归一化算法：
 * <ol>
 *   <li>把 title 做 normalize key：lowercase + 去标点 + 去常见后缀（架构/部署/概述/简介/FC/...）+ 去空白；</li>
 *   <li>精确命中 → 返回已注册的 canonical slug；</li>
 *   <li>包含命中（短包含于长且字符长度差 ≤ 3） → 返回较短的 canonical；</li>
 *   <li>未命中 → 注册一条新别名（draft slug 作为 canonical）。</li>
 * </ol>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityAliasService {

    /** 与领域语义无关、单纯用于"同义识别"的常见后缀/修饰词 */
    private static final List<String> NOISE_SUFFIXES = List.of(
            "架构", "概述", "简介", "介绍", "部署", "实践", "说明", "原理", "机制", "方案",
            "概念", "技术", "服务", "平台", "系统", "模块", "组件",
            "architecture", "overview", "introduction", "deployment", "service", "platform"
    );

    /** 噪音括号 / 标点（中英文） */
    private static final Pattern PUNCT = Pattern.compile("[\\p{Punct}\\s\\u3000-\\u303F\\uFF00-\\uFFEF]+");

    private final StorageProperties storageProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** normalizedKey -> canonicalSlug */
    private final Map<String, String> keyToCanonical = new ConcurrentHashMap<>();
    /** canonicalSlug -> Entry */
    private final Map<String, AliasEntry> entries = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        File f = file();
        if (f.exists()) {
            try {
                Snapshot s = objectMapper.readValue(f, Snapshot.class);
                if (s.entries != null) {
                    entries.putAll(s.entries);
                    for (AliasEntry e : s.entries.values()) {
                        for (String k : e.keys) {
                            keyToCanonical.put(k, e.canonicalSlug);
                        }
                    }
                }
                log.info("加载实体别名表: canonical={} aliases={}", entries.size(), keyToCanonical.size());
            } catch (Exception ex) {
                log.warn("加载实体别名表失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 把 (type, title, draftSlug) 归一化到 canonical slug；如果已存在等价实体则返回老的 slug。
     */
    public synchronized String canonicalize(String type, String title, String draftSlug) {
        String safeTitle = title == null ? draftSlug : title;
        String key = normalizeKey(safeTitle);
        if (key.isEmpty()) {
            // 退化：用 slug 兜底，避免 source 类节点被错误合并
            return draftSlug;
        }

        // source 类型的页面不参与归一化（每个源是一个独立节点）
        if ("source".equalsIgnoreCase(type)) {
            return draftSlug;
        }

        // 1. 精确命中
        String hit = keyToCanonical.get(key);
        if (hit != null) {
            registerAlias(hit, key, safeTitle);
            return hit;
        }

        // 2. 包含命中：找已有 key 与当前 key 互相包含且长度差很小
        for (Map.Entry<String, String> e : keyToCanonical.entrySet()) {
            String existing = e.getKey();
            if (existing.isEmpty()) {
                continue;
            }
            int la = existing.length();
            int lb = key.length();
            int diff = Math.abs(la - lb);
            // 双向包含且差异不大；强约束：短串至少 2 个字以上，避免误合并
            if (diff <= 3 && Math.min(la, lb) >= 2
                    && (existing.contains(key) || key.contains(existing))) {
                String canonical = e.getValue();
                registerAlias(canonical, key, safeTitle);
                log.debug("别名归并: title='{}' (key='{}') -> canonical='{}'", safeTitle, key, canonical);
                return canonical;
            }
        }

        // 3. 未命中：用 draftSlug 作为 canonical 注册
        AliasEntry entry = entries.computeIfAbsent(draftSlug, k -> {
            AliasEntry x = new AliasEntry();
            x.canonicalSlug = draftSlug;
            x.canonicalTitle = safeTitle;
            return x;
        });
        entry.keys.add(key);
        entry.titles.add(safeTitle);
        keyToCanonical.put(key, draftSlug);
        return draftSlug;
    }

    private void registerAlias(String canonical, String key, String title) {
        AliasEntry e = entries.computeIfAbsent(canonical, k -> {
            AliasEntry x = new AliasEntry();
            x.canonicalSlug = canonical;
            x.canonicalTitle = title;
            return x;
        });
        e.keys.add(key);
        e.titles.add(title);
        keyToCanonical.put(key, canonical);
    }

    /**
     * 重新解析一段已生成的 outLink 列表，把每个 link 都映射到 canonical（若有）。
     */
    public List<String> canonicalizeLinks(List<String> rawLinks) {
        if (rawLinks == null || rawLinks.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String l : rawLinks) {
            if (l == null || l.isBlank()) {
                continue;
            }
            String slug = l.trim();
            // 已经是注册过的 canonical
            if (entries.containsKey(slug)) {
                if (seen.add(slug)) {
                    result.add(slug);
                }
                continue;
            }
            // 把 slug 当作 title 反推 key 试探匹配
            String key = normalizeKey(slug);
            String hit = keyToCanonical.get(key);
            String target = hit != null ? hit : slug;
            if (seen.add(target)) {
                result.add(target);
            }
        }
        return result;
    }

    public synchronized void persist() {
        File f = file();
        try {
            f.getParentFile().mkdirs();
            Snapshot s = new Snapshot();
            s.entries = new HashMap<>(entries);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(f, s);
        } catch (Exception e) {
            log.warn("持久化别名表失败: {}", e.getMessage());
        }
    }

    /** 清空（reset 时调用）。 */
    public synchronized void clear() {
        entries.clear();
        keyToCanonical.clear();
        File f = file();
        if (f.exists()) {
            f.delete();
        }
    }

    /**
     * 计算用于同义识别的归一化 key：lowercase + 去标点空白 + 去末尾常见修饰词。
     */
    public static String normalizeKey(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim().toLowerCase(Locale.ROOT);
        s = PUNCT.matcher(s).replaceAll("");
        // 移除末尾常见修饰词
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suf : NOISE_SUFFIXES) {
                if (s.endsWith(suf) && s.length() > suf.length() + 1) {
                    s = s.substring(0, s.length() - suf.length());
                    changed = true;
                }
            }
        }
        return s;
    }

    private File file() {
        return new File(storageProperties.getGraphDir(), "aliases.json");
    }

    @Data
    public static class AliasEntry {
        public String canonicalSlug;
        public String canonicalTitle;
        public Set<String> keys = new HashSet<>();
        public Set<String> titles = new HashSet<>();
    }

    @Data
    public static class Snapshot {
        public Map<String, AliasEntry> entries;
    }
}
