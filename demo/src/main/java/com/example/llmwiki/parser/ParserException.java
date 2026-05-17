package com.example.llmwiki.parser;

/**
 * 解析异常。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
public class ParserException extends RuntimeException {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
