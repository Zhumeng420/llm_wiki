package com.example.llmwiki.queue;

import com.example.llmwiki.config.IngestProperties;
import com.example.llmwiki.config.StorageProperties;
import com.example.llmwiki.domain.IngestTask;
import com.example.llmwiki.domain.SourceRecord;
import com.example.llmwiki.ingest.IngestPipeline;
import com.example.llmwiki.progress.ProgressBus;
import com.example.llmwiki.progress.ProgressEvent;
import com.example.llmwiki.repository.IngestTaskRepository;
import com.example.llmwiki.repository.SourceRecordRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 摄入队列：DB 持久化 + 单线程串行 worker + 取消标志 + 失败重试。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestQueueService {

    private final IngestTaskRepository taskRepo;
    private final SourceRecordRepository sourceRepo;
    private final IngestPipeline pipeline;
    private final ProgressBus progressBus;
    private final IngestProperties ingestProperties;
    private final StorageProperties storageProperties;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ingest-worker");
        t.setDaemon(true);
        return t;
    });

    private final Set<Long> cancelled = ConcurrentHashMap.newKeySet();

    @PostConstruct
    public void recover() {
        // 启动恢复：把 RUNNING 的任务标记为 PENDING 重新入队
        for (IngestTask t : taskRepo.findByStatusOrderByIdAsc("RUNNING")) {
            t.setStatus("PENDING");
            taskRepo.save(t);
        }
        for (IngestTask t : taskRepo.findByStatusOrderByIdAsc("PENDING")) {
            scheduleExecution(t.getId());
        }
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdownNow();
    }

    /**
     * 注册一个来源并入队。
     */
    public IngestTask enqueueFile(String displayName, byte[] bytes, String mime) {
        SourceRecord src = sourceRepo.findByKindAndRef("FILE", displayName).orElseGet(() ->
                SourceRecord.builder().kind("FILE").ref(displayName).displayName(displayName)
                        .createdAt(Instant.now()).watchEnabled(false).build());
        sourceRepo.save(src);

        // 持久化文件到 raw/
        try {
            File raw = new File(storageProperties.getRawDir(), src.getId() + "_" + displayName);
            raw.getParentFile().mkdirs();
            Files.write(raw.toPath(), bytes);
        } catch (Exception e) {
            log.warn("写 raw 失败: {}", e.getMessage());
        }

        IngestTask task = createTask(src.getId());
        scheduleExecutionWithBytes(task.getId(), bytes, mime);
        return task;
    }

    public IngestTask enqueueUrl(String url, boolean watch) {
        SourceRecord src = sourceRepo.findByKindAndRef("URL", url).orElseGet(() ->
                SourceRecord.builder().kind("URL").ref(url).displayName(url)
                        .createdAt(Instant.now()).build());
        src.setWatchEnabled(watch);
        sourceRepo.save(src);
        IngestTask task = createTask(src.getId());
        scheduleExecution(task.getId());
        return task;
    }

    public IngestTask enqueueRemote(String kind, String ref, String displayName, boolean watch) {
        SourceRecord src = sourceRepo.findByKindAndRef(kind, ref).orElseGet(() ->
                SourceRecord.builder().kind(kind).ref(ref).displayName(displayName)
                        .createdAt(Instant.now()).build());
        src.setWatchEnabled(watch);
        sourceRepo.save(src);
        IngestTask task = createTask(src.getId());
        scheduleExecution(task.getId());
        return task;
    }

    public void cancel(Long taskId) {
        cancelled.add(taskId);
        taskRepo.findById(taskId).ifPresent(t -> {
            if ("PENDING".equals(t.getStatus())) {
                t.setStatus("CANCELLED");
                t.setFinishedAt(Instant.now());
                taskRepo.save(t);
            }
        });
    }

    public void retry(Long taskId) {
        taskRepo.findById(taskId).ifPresent(t -> {
            t.setStatus("PENDING");
            t.setRetryCount(0);
            t.setErrorMessage(null);
            taskRepo.save(t);
            scheduleExecution(t.getId());
        });
    }

    private IngestTask createTask(Long sourceId) {
        IngestTask t = IngestTask.builder()
                .sourceId(sourceId)
                .status("PENDING")
                .stage("QUEUED")
                .percent(0)
                .retryCount(0)
                .createdAt(Instant.now())
                .build();
        t = taskRepo.save(t);
        progressBus.publish(ProgressEvent.builder().taskId(t.getId())
                .stage("QUEUED").percent(0).status("PENDING").message("已入队").build());
        return t;
    }

    private void scheduleExecution(Long taskId) {
        scheduleExecutionWithBytes(taskId, null, null);
    }

    private void scheduleExecutionWithBytes(Long taskId, byte[] bytes, String mime) {
        worker.submit(() -> execute(taskId, bytes, mime));
    }

    private void execute(Long taskId, byte[] bytes, String mime) {
        IngestTask task = taskRepo.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        if (cancelled.contains(taskId)) {
            task.setStatus("CANCELLED");
            taskRepo.save(task);
            return;
        }
        SourceRecord src = sourceRepo.findById(task.getSourceId()).orElse(null);
        if (src == null) {
            task.setStatus("FAILED");
            task.setErrorMessage("source not found");
            taskRepo.save(task);
            return;
        }

        task.setStatus("RUNNING");
        task.setStartedAt(Instant.now());
        taskRepo.save(task);

        try {
            byte[] use = bytes;
            if (use == null && "FILE".equals(src.getKind())) {
                File raw = new File(storageProperties.getRawDir(), src.getId() + "_" + src.getDisplayName());
                if (raw.exists()) {
                    use = Files.readAllBytes(raw.toPath());
                }
            }
            pipeline.run(taskId, src, use, mime);
            task.setStatus("SUCCESS");
            task.setPercent(100);
            task.setFinishedAt(Instant.now());
            taskRepo.save(task);
        } catch (Exception ex) {
            int retry = task.getRetryCount() == null ? 0 : task.getRetryCount();
            int max = ingestProperties.getIngest().getMaxRetry();
            log.warn("任务 {} 失败 retry={}/{} : {}", taskId, retry, max, ex.getMessage(), ex);
            task.setErrorMessage(ex.getMessage());
            if (retry + 1 < max) {
                task.setRetryCount(retry + 1);
                task.setStatus("PENDING");
                taskRepo.save(task);
                scheduleExecutionWithBytes(taskId, bytes, mime);
            } else {
                task.setStatus("FAILED");
                task.setFinishedAt(Instant.now());
                taskRepo.save(task);
                progressBus.publish(ProgressEvent.builder().taskId(taskId)
                        .stage("FAIL").percent(0).status("FAILED").message(ex.getMessage()).build());
            }
        }
    }
}
