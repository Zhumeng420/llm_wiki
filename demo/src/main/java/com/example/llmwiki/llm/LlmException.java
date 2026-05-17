package com.example.llmwiki.llm;

/**
 * LLM 调用异常。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
