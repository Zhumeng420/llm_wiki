package com.example.llmwiki.progress;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进度事件总线：维护订阅者列表 + 广播 ProgressEvent。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Component
public class ProgressBus {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 最近 50 条事件，便于新订阅者获取近期上下文 */
    private final java.util.Deque<ProgressEvent> recent = new java.util.concurrent.ConcurrentLinkedDeque<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        // replay 最近事件
        for (ProgressEvent e : recent) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(e));
            } catch (Exception ignore) {
                // ignore
            }
        }
        return emitter;
    }

    public void publish(ProgressEvent event) {
        if (recent.size() > 50) {
            recent.pollFirst();
        }
        recent.addLast(event);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(event));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    public List<ProgressEvent> recent() {
        return recent.stream().toList();
    }
}
