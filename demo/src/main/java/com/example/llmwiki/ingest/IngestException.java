package com.example.llmwiki.ingest;

/**
 * 摄入异常。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
public class IngestException extends RuntimeException {
    public IngestException(String msg) {
        super(msg);
    }

    public IngestException(String msg, Throwable c) {
        super(msg, c);
    }
}
