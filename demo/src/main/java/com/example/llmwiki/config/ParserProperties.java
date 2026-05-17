package com.example.llmwiki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 解析器相关配置（飞书 / 钉钉 / OCR）。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm-wiki.parser")
public class ParserProperties {

    private Feishu feishu = new Feishu();
    private DingTalk dingtalk = new DingTalk();
    private Ocr ocr = new Ocr();

    @Data
    public static class Feishu {
        private Boolean enabled = false;
        private String appId = "";
        private String appSecret = "";
    }

    @Data
    public static class DingTalk {
        private Boolean enabled = false;
        private String appKey = "";
        private String appSecret = "";
    }

    @Data
    public static class Ocr {
        /** 是否启用 OCR（需配置 tesseract 数据路径） */
        private Boolean enabled = false;
        /** tesseract tessdata 路径 */
        private String dataPath = "";
        /** 识别语言，如 chi_sim+eng */
        private String lang = "chi_sim+eng";
    }
}
