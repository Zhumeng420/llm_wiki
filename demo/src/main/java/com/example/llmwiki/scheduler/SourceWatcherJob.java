package com.example.llmwiki.scheduler;

import com.example.llmwiki.config.IngestProperties;
import com.example.llmwiki.domain.IngestTask;
import com.example.llmwiki.domain.SourceRecord;
import com.example.llmwiki.queue.IngestQueueService;
import com.example.llmwiki.repository.SourceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 定时来源刷新 Job：扫描 watchEnabled=true 的来源，重新入队。
 * <p>
 * 支持的来源类型：URL（网页）/ FEISHU / DINGTALK 等远端动态来源。文件来源不会被定时刷新。
 * </p>
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class SourceWatcherJob implements Job {

    private final SourceRecordRepository sourceRepo;
    private final IngestQueueService ingestQueue;
    private final IngestProperties ingestProperties;

    @Override
    public void execute(JobExecutionContext context) {
        if (!Boolean.TRUE.equals(ingestProperties.getScheduler().getEnabled())) {
            log.info("scheduler 已关闭，跳过本轮刷新");
            return;
        }

        List<SourceRecord> watched = sourceRepo.findByWatchEnabledTrue();
        log.info("开始定时刷新 watched 来源: count={}", watched.size());
        for (SourceRecord src : watched) {
            try {
                IngestTask task;
                if ("URL".equalsIgnoreCase(src.getKind())) {
                    task = ingestQueue.enqueueUrl(src.getRef(), true);
                } else if ("FEISHU".equalsIgnoreCase(src.getKind())
                        || "DINGTALK".equalsIgnoreCase(src.getKind())) {
                    task = ingestQueue.enqueueRemote(src.getKind(), src.getRef(),
                            src.getDisplayName(), true);
                } else {
                    continue;
                }
                src.setLastFetchedAt(Instant.now());
                sourceRepo.save(src);
                log.info("已刷新来源: id={} kind={} ref={} taskId={}",
                        src.getId(), src.getKind(), src.getRef(), task.getId());
            } catch (Exception e) {
                log.warn("刷新来源失败 id={} ref={} : {}", src.getId(), src.getRef(), e.getMessage());
            }
        }
    }
}
