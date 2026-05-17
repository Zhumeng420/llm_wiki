package com.example.llmwiki.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 通用字符串/哈希工具。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
public final class TextUtils {

    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{L}\\p{N}\\-\\u4e00-\\u9fa5]+");
    private static final Pattern WHITESPACES = Pattern.compile("\\s+");

    private TextUtils() {
    }

    /**
     * 计算 SHA256 十六进制串。
     */
    public static String sha256(String text) {
        if (text == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 把任意标题转为安全的 slug（保留中英文、数字、连字符）。
     */
    public static String slugify(String title) {
        if (title == null || title.isBlank()) {
            return "page-" + System.currentTimeMillis();
        }
        String s = Normalizer.normalize(title.trim(), Normalizer.Form.NFKC);
        s = s.toLowerCase(Locale.ROOT).replace(' ', '-');
        s = NON_ALNUM.matcher(s).replaceAll("-");
        s = s.replaceAll("-+", "-");
        if (s.startsWith("-")) {
            s = s.substring(1);
        }
        if (s.endsWith("-")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) {
            s = "page-" + System.currentTimeMillis();
        }
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    public static String normalizeWhitespace(String s) {
        if (s == null) {
            return "";
        }
        return WHITESPACES.matcher(s).replaceAll(" ").trim();
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
