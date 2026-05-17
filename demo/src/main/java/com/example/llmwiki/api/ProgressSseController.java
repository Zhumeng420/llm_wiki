package com.example.llmwiki.api;

import com.example.llmwiki.progress.ProgressBus;
import com.example.llmwiki.progress.ProgressEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 摄入进度 SSE 推送。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressSseController {

    private final ProgressBus progressBus;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return progressBus.subscribe();
    }

    @GetMapping("/recent")
    public List<ProgressEvent> recent() {
        return progressBus.recent();
    }
}
