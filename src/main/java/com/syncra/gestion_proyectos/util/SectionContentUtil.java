package com.syncra.gestion_proyectos.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SectionContentUtil {

    private static final String SECTION_ATTR = "data-section-key";
    private static final String SECTION_TITLE_ATTR = "data-section-title";

    private SectionContentUtil() {
    }

    public static String wrapSection(String sectionKey, String title, String html) {
        String contenido = (html == null || html.isBlank()) ? "<p></p>" : html;

        return "<div " + SECTION_ATTR + "=\"" + escapeAttr(sectionKey) + "\" "
                + SECTION_TITLE_ATTR + "=\"" + escapeAttr(title) + "\">" + contenido + "</div>";
    }

    public static class ExtractedSection {

        private final String title;
        private final String content;

        public ExtractedSection(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }


    public static ExtractedSection extractSection(String html, String sectionKey) {
        if (html == null || html.isBlank() || sectionKey == null || sectionKey.isBlank()) {
            return null;
        }

        Pattern openTagPattern = Pattern.compile(
                "<div[^>]*" + SECTION_ATTR + "=\"" + Pattern.quote(sectionKey) + "\"[^>]*>",
                Pattern.CASE_INSENSITIVE);

        Matcher openMatcher = openTagPattern.matcher(html);

        if (!openMatcher.find()) {
            return null;
        }

        String openTag = openMatcher.group();
        String title = extractAttr(openTag, SECTION_TITLE_ATTR);

        int contentStart = openMatcher.end();
        int contentEnd = findMatchingCloseTag(html, contentStart);

        if (contentEnd == -1) {
            return null;
        }

        String innerHtml = html.substring(contentStart, contentEnd);

        if (!hasVisibleContent(innerHtml)) {
            return null;
        }

        return new ExtractedSection(title != null ? title : sectionKey, innerHtml);
    }

   
    private static int findMatchingCloseTag(String html, int from) {
        Pattern tagPattern = Pattern.compile("<div[^>]*>|</div>", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(html);
        tagMatcher.region(from, html.length());

        int depth = 1;

        while (tagMatcher.find()) {
            String tag = tagMatcher.group();

            if (tag.startsWith("</")) {
                depth--;

                if (depth == 0) {
                    return tagMatcher.start();
                }
            } else {
                depth++;
            }
        }

        return -1;
    }

    private static boolean hasVisibleContent(String html) {
        String texto = html.replaceAll("<[^>]*>", "").trim();
        return !texto.isEmpty();
    }

    private static String extractAttr(String tag, String attrName) {
        Matcher matcher = Pattern.compile(attrName + "=\"([^\"]*)\"").matcher(tag);

        if (!matcher.find()) {
            return null;
        }

        return unescapeAttr(matcher.group(1));
    }

    private static String escapeAttr(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String unescapeAttr(String value) {
        return value
                .replace("&quot;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}