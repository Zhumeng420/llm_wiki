package com.example.llmwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 摄入与调度配置。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm-wiki")
public class IngestProperties {

    private Ingest ingest = new Ingest();
    private Scheduler scheduler = new Scheduler();

    @Data
    public static class Ingest {
        private Integer maxRetry = 3;
        private Integer workerThreads = 1;
    }

    @Data
    public static class Scheduler {
        private Boolean enabled = true;
        private String cron = "0 0 3 * * ?";
    }
}
