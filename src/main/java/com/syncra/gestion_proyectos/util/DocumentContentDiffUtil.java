package com.syncra.gestion_proyectos.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Compara dos versiones HTML del editor por bloques (párrafos, encabezados,
 * listas, secciones), no carácter a carácter, y produce una descripción
 * legible más un ancla para navegar en el documento.
 */
public final class DocumentContentDiffUtil {

    public static final int MAX_DESCRIPTION_LENGTH = 1000;

    private DocumentContentDiffUtil() {
    }

    public static final class DiffResult {
        private final String description;
        private final String targetSnippet;
        private final String targetSectionKey;
        private final String changeKind;

        public DiffResult(String description, String targetSnippet, String targetSectionKey, String changeKind) {
            this.description = description;
            this.targetSnippet = targetSnippet;
            this.targetSectionKey = targetSectionKey;
            this.changeKind = changeKind;
        }

        public String getDescription() {
            return description;
        }

        public String getTargetSnippet() {
            return targetSnippet;
        }

        public String getTargetSectionKey() {
            return targetSectionKey;
        }

        public String getChangeKind() {
            return changeKind;
        }
    }

    private static final class Block {
        private final String tag;
        private final String text;
        private final String sectionKey;
        private final String sectionTitle;

        private Block(String tag, String text, String sectionKey, String sectionTitle) {
            this.tag = tag;
            this.text = text;
            this.sectionKey = sectionKey;
            this.sectionTitle = sectionTitle;
        }

        private String fingerprint() {
            return tag + "\n" + text;
        }
    }

    public static DiffResult describe(String previousHtml, String currentHtml) {
        List<Block> previous = extractBlocks(previousHtml);
        List<Block> current = extractBlocks(currentHtml);

        if (fingerprints(previous).equals(fingerprints(current))) {
            return new DiffResult("modificó el formato del contenido", null, null, "FORMAT");
        }

        List<String> descriptions = new ArrayList<>();
        String targetSnippet = null;
        String targetSectionKey = null;
        String changeKind = "MODIFIED";

        int i = 0;
        int j = 0;
        Set<String> remainingOld = new HashSet<>(fingerprints(previous));
        Set<String> remainingNew = new HashSet<>(fingerprints(current));

        while (i < previous.size() || j < current.size()) {
            if (i < previous.size() && j < current.size()
                    && previous.get(i).fingerprint().equals(current.get(j).fingerprint())) {
                remainingOld.remove(previous.get(i).fingerprint());
                remainingNew.remove(current.get(j).fingerprint());
                i++;
                j++;
                continue;
            }

            boolean oldLaterInNew = i < previous.size() && remainingNew.contains(previous.get(i).fingerprint());
            boolean newLaterInOld = j < current.size() && remainingOld.contains(current.get(j).fingerprint());

            if (i < previous.size() && j < current.size() && !oldLaterInNew && !newLaterInOld) {
                Block before = previous.get(i);
                Block after = current.get(j);
                descriptions.add(describirModificacion(before, after));
                if (targetSnippet == null) {
                    targetSnippet = recortar(after.text);
                    targetSectionKey = blankToNull(after.sectionKey);
                    changeKind = "MODIFIED";
                }
                remainingOld.remove(before.fingerprint());
                remainingNew.remove(after.fingerprint());
                i++;
                j++;
            } else if (j < current.size() && (i >= previous.size() || !newLaterInOld)) {
                Block added = current.get(j);
                descriptions.add(describirAlta(added));
                if (targetSnippet == null) {
                    targetSnippet = recortar(added.text);
                    targetSectionKey = blankToNull(added.sectionKey);
                    changeKind = "ADDED";
                }
                remainingNew.remove(added.fingerprint());
                j++;
            } else if (i < previous.size()) {
                Block removed = previous.get(i);
                descriptions.add(describirBaja(removed));
                if (targetSnippet == null) {
                    targetSnippet = recortar(removed.text);
                    targetSectionKey = blankToNull(removed.sectionKey);
                    changeKind = "REMOVED";
                }
                remainingOld.remove(removed.fingerprint());
                i++;
            }
        }

        if (descriptions.isEmpty()) {
            return new DiffResult("modificó el contenido del documento", null, null, "MODIFIED");
        }

        String joined = String.join("; ", descriptions.stream().limit(4).toList());
        if (descriptions.size() > 4) {
            joined += " y " + (descriptions.size() - 4) + " cambio(s) más";
        }
        return new DiffResult(truncar(joined), targetSnippet, targetSectionKey, changeKind);
    }

    private static List<String> fingerprints(List<Block> blocks) {
        return blocks.stream().map(Block::fingerprint).toList();
    }

    private static List<Block> extractBlocks(String html) {
        List<Block> blocks = new ArrayList<>();
        Document document = Jsoup.parseBodyFragment(html == null ? "" : html);
        walk(document.body(), null, null, blocks);
        return blocks;
    }

    private static void walk(Element parent, String sectionKey, String sectionTitle, List<Block> out) {
        for (Element child : parent.children()) {
            if (child.hasAttr("data-section-key")) {
                String key = child.attr("data-section-key");
                String title = child.attr("data-section-title");
                if (title == null || title.isBlank()) {
                    title = primerEncabezado(child);
                }
                walk(child, key, title, out);
                if (child.children().isEmpty()) {
                    String label = (title == null || title.isBlank()) ? key : title;
                    out.add(new Block("section", label, key, label));
                }
                continue;
            }

            String tag = child.tagName().toLowerCase();
            if ("ul".equals(tag) || "ol".equals(tag) || "div".equals(tag) || "section".equals(tag)) {
                if (child.hasAttr("data-document-card") || child.hasAttr("data-file-card")
                        || child.hasAttr("data-syncra-toc")) {
                    String text = textoDeBloque(child);
                    if (!text.isBlank()) {
                        out.add(new Block(tag, text, sectionKey, sectionTitle));
                    }
                    continue;
                }
                walk(child, sectionKey, sectionTitle, out);
                continue;
            }

            if (esBloque(tag) || child.selectFirst("img") != null) {
                String text = textoDeBloque(child);
                if (text.isBlank()) {
                    continue;
                }
                out.add(new Block(tag, text, sectionKey, sectionTitle));
            }
        }
    }

    private static boolean esBloque(String tag) {
        return tag.equals("p") || tag.equals("h1") || tag.equals("h2") || tag.equals("h3")
                || tag.equals("h4") || tag.equals("h5") || tag.equals("h6") || tag.equals("li")
                || tag.equals("blockquote") || tag.equals("pre") || tag.equals("table")
                || tag.equals("hr") || tag.equals("img") || tag.equals("figure");
    }

    private static String textoDeBloque(Element element) {
        if (element.hasAttr("data-titulo")) {
            return element.attr("data-titulo").trim();
        }
        if (element.hasAttr("data-file-name")) {
            return element.attr("data-file-name").trim();
        }
        String text = element.text().replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        if (!text.isBlank()) {
            return text;
        }
        if (element.selectFirst("img") != null || "img".equals(element.tagName())) {
            return "[imagen]";
        }
        if ("table".equals(element.tagName())) {
            return "[tabla]";
        }
        if ("hr".equals(element.tagName())) {
            return "[separador]";
        }
        return "";
    }

    private static String primerEncabezado(Element section) {
        Element heading = section.selectFirst("h1, h2, h3, h4");
        return heading == null ? "" : heading.text().trim();
    }

    private static String describirAlta(Block block) {
        if (esTitulo(block.tag)) {
            return "se agregó el título \"" + recortar(block.text) + "\"";
        }
        if ("li".equals(block.tag)) {
            return "se agregó el elemento de lista \"" + recortar(block.text) + "\"";
        }
        if (block.sectionTitle != null && !block.sectionTitle.isBlank()
                && block.sectionTitle.equals(block.text)) {
            return "se agregó la sección \"" + recortar(block.sectionTitle) + "\"";
        }
        return "se agregó el párrafo \"" + recortar(block.text) + "\"";
    }

    private static String describirBaja(Block block) {
        if (esTitulo(block.tag)) {
            return "se eliminó el título \"" + recortar(block.text) + "\"";
        }
        if ("li".equals(block.tag)) {
            return "se eliminó el elemento de lista \"" + recortar(block.text) + "\"";
        }
        if (block.sectionTitle != null && !block.sectionTitle.isBlank()) {
            return "se eliminó la sección \"" + recortar(block.sectionTitle) + "\"";
        }
        return "se eliminó el párrafo \"" + recortar(block.text) + "\"";
    }

    private static String describirModificacion(Block before, Block after) {
        if (esTitulo(before.tag) || esTitulo(after.tag)) {
            return "se modificó el título de \"" + recortar(before.text) + "\" a \"" + recortar(after.text) + "\"";
        }
        if (before.sectionTitle != null && !before.sectionTitle.isBlank()
                && !before.text.equals(after.text) && before.sectionTitle.equals(before.text)) {
            return "se modificó la sección \"" + recortar(before.sectionTitle) + "\"";
        }
        return "se modificó el párrafo \"" + recortar(before.text) + "\"";
    }

    private static boolean esTitulo(String tag) {
        return tag != null && tag.startsWith("h") && tag.length() == 2;
    }

    private static String recortar(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 77) + "...";
    }

    private static String truncar(String text) {
        if (text.length() <= MAX_DESCRIPTION_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
