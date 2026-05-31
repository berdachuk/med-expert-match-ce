package com.berdachuk.medexpertmatch.web.service.impl;

import com.berdachuk.medexpertmatch.web.service.ChatMarkdownRenderer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight CommonMark subset renderer with an HTML allowlist for SSR chat history.
 */
@Service("chatMarkdownRenderer")
public class ChatMarkdownRendererImpl implements ChatMarkdownRenderer {

    private static final Pattern FENCED_CODE = Pattern.compile("```(?:\\w+)?\\n([\\s\\S]*?)```");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\*)([^*]+)\\*(?!\\*)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)]\\(((?:[^()]|\\([^()]*\\))*)\\)");
    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z0-9]+)(\\s[^>]*)?>");
    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);

    private static final List<String> ALLOWED_TAGS = List.of(
            "p", "strong", "em", "code", "pre", "ul", "ol", "li", "a", "br");

    @Override
    public String renderSafe(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        return sanitizeHtml(convertMarkdown(markdown.trim()));
    }

    private String convertMarkdown(String text) {
        String escaped = escapeHtml(text);
        escaped = replaceAll(FENCED_CODE, escaped, m -> "<pre><code>" + m.group(1) + "</code></pre>");
        escaped = applyInlineFormatting(escaped);

        StringBuilder html = new StringBuilder();
        boolean inList = false;
        for (String line : escaped.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ")) {
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(trimmed.substring(2).trim()).append("</li>");
                continue;
            }
            if (inList) {
                html.append("</ul>");
                inList = false;
            }
            if (!trimmed.isEmpty()) {
                html.append("<p>").append(trimmed).append("</p>");
            }
        }
        if (inList) {
            html.append("</ul>");
        }
        return html.toString();
    }

    private String applyInlineFormatting(String text) {
        String result = replaceAll(INLINE_CODE, text, m -> "<code>" + m.group(1) + "</code>");
        for (int i = 0; i < 3; i++) {
            String next = replaceAll(BOLD, result, m -> "<strong>" + m.group(1) + "</strong>");
            next = replaceAll(LINK, next, m -> {
                String href = m.group(2).trim();
                if (!href.startsWith("http://") && !href.startsWith("https://")) {
                    return m.group(1);
                }
                return "<a href=\"" + escapeAttribute(href) + "\">" + m.group(1) + "</a>";
            });
            next = replaceAll(ITALIC, next, m -> "<em>" + m.group(1) + "</em>");
            if (next.equals(result)) {
                break;
            }
            result = next;
        }
        return result;
    }

    private String sanitizeHtml(String html) {
        Matcher matcher = TAG.matcher(html);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(html, last, matcher.start());
            String slash = matcher.group(1);
            String tag = matcher.group(2).toLowerCase(Locale.ROOT);
            String attrs = matcher.group(3) == null ? "" : matcher.group(3);
            if (ALLOWED_TAGS.contains(tag)) {
                if ("a".equals(tag) && slash.isEmpty()) {
                    out.append(buildSafeAnchor(attrs));
                } else if ("a".equals(tag)) {
                    out.append("</a>");
                } else if (slash.isEmpty()) {
                    out.append('<').append(tag).append('>');
                } else {
                    out.append("</").append(tag).append('>');
                }
            }
            last = matcher.end();
        }
        out.append(html.substring(last));
        return out.toString();
    }

    private String buildSafeAnchor(String attrs) {
        Matcher hrefMatcher = HREF.matcher(attrs);
        if (!hrefMatcher.find()) {
            return "";
        }
        String href = hrefMatcher.group(1).trim();
        if (!href.startsWith("http://") && !href.startsWith("https://")) {
            return "";
        }
        return "<a href=\"" + escapeAttribute(href) + "\">";
    }

    private static String replaceAll(Pattern pattern, String input, java.util.function.Function<Matcher, String> replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacer.apply(matcher)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String escapeAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }
}
