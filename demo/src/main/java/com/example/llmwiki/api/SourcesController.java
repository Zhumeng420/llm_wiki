package com.example.llmwiki.api;

import com.example.llmwiki.domain.IngestTask;
import com.example.llmwiki.domain.SourceRecord;
import com.example.llmwiki.queue.IngestQueueService;
import com.example.llmwiki.repository.IngestTaskRepository;
import com.example.llmwiki.repository.SourceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 数据源接口：上传文件 / 注册 URL / 飞书钉钉 / 列出来源 / 任务列表 / 取消重试。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/sources")
@RequiredArgsConstructor
public class SourcesController {

    private final IngestQueueService queueService;
    private final SourceRecordRepository sourceRepo;
    private final IngestTaskRepository taskRepo;

    @GetMapping
    public List<SourceRecord> list() {
        return sourceRepo.findAll();
    }

    @PostMapping("/file")
    public IngestTask uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        return queueService.enqueueFile(file.getOriginalFilename(), file.getBytes(), file.getContentType());
    }

    @PostMapping("/url")
    public IngestTask submitUrl(@RequestBody UrlRequest req) {
        return queueService.enqueueUrl(req.getUrl(), Boolean.TRUE.equals(req.getWatch()));
    }

    @PostMapping("/remote")
    public IngestTask submitRemote(@RequestBody RemoteRequest req) {
        String displayName = req.getDisplayName() == null || req.getDisplayName().isBlank()
                ? req.getRef() : req.getDisplayName();
        return queueService.enqueueRemote(req.getKind(), req.getRef(), displayName,
                Boolean.TRUE.equals(req.getWatch()));
    }

    @GetMapping("/tasks")
    public List<IngestTask> tasks() {
        return taskRepo.findTop50ByOrderByIdDesc();
    }

    @PostMapping("/tasks/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Long id) {
        queueService.cancel(id);
        return Map.of("ok", true);
    }

    @PostMapping("/tasks/{id}/retry")
    public Map<String, Object> retry(@PathVariable Long id) {
        queueService.retry(id);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        sourceRepo.deleteById(id);
        return Map.of("ok", true);
    }

    @lombok.Data
    public static class UrlRequest {
        private String url;
        private Boolean watch;
    }

    @lombok.Data
    public static class RemoteRequest {
        /** FEISHU / DINGTALK */
        private String kind;
        /** 文档 token / id / 链接 */
        private String ref;
        private String displayName;
        private Boolean watch;
    }
}
