package com.example.llmwiki.api;

import com.example.llmwiki.config.IngestProperties;
import com.example.llmwiki.domain.SourceRecord;
import com.example.llmwiki.repository.SourceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 调度接口：列出 watched 来源 / 立即执行一次 / 切换 watch 标志 / 修改 cron。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final IngestProperties ingestProperties;
    private final SourceRecordRepository sourceRepo;
    private final Scheduler scheduler;

    @GetMapping("/config")
    public IngestProperties.Scheduler config() {
        return ingestProperties.getScheduler();
    }

    @PostMapping("/config")
    public Map<String, Object> updateConfig(@RequestBody IngestProperties.Scheduler cfg) {
        if (cfg.getCron() != null && !cfg.getCron().isBlank()) {
            ingestProperties.getScheduler().setCron(cfg.getCron());
        }
        if (cfg.getEnabled() != null) {
            ingestProperties.getScheduler().setEnabled(cfg.getEnabled());
        }
        return Map.of("ok", true, "scheduler", ingestProperties.getScheduler());
    }

    @GetMapping("/watched")
    public List<SourceRecord> watched() {
        return sourceRepo.findByWatchEnabledTrue();
    }

    /**
     * 切换某个来源的 watchEnabled 开关。
     */
    @PostMapping("/sources/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return sourceRepo.findById(id).map(src -> {
            src.setWatchEnabled(Boolean.TRUE.equals(body.get("enabled")));
            sourceRepo.save(src);
            return Map.<String, Object>of("ok", true, "watchEnabled", src.getWatchEnabled());
        }).orElseGet(() -> Map.of("ok", false, "error", "not found"));
    }

    /**
     * 立即触发一次 SourceWatcherJob。
     */
    @PostMapping("/run-now")
    public Map<String, Object> runNow() throws SchedulerException {
        scheduler.triggerJob(new JobKey("sourceWatcherJob"));
        return Map.of("ok", true);
    }
}
