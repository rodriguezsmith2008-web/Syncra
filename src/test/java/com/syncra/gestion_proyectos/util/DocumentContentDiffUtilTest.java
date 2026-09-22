package com.syncra.gestion_proyectos.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DocumentContentDiffUtilTest {

    @Test
    void describeAddedParagraph() {
        DocumentContentDiffUtil.DiffResult result = DocumentContentDiffUtil.describe(
                "<p>Hola</p>",
                "<p>Hola</p><p>Mundo nuevo</p>");

        assertTrue(result.getDescription().contains("se agregó el párrafo"));
        assertTrue(result.getDescription().contains("Mundo nuevo"));
        assertEquals("ADDED", result.getChangeKind());
        assertEquals("Mundo nuevo", result.getTargetSnippet());
    }

    @Test
    void describeTitleChange() {
        DocumentContentDiffUtil.DiffResult result = DocumentContentDiffUtil.describe(
                "<h1>Antes</h1><p>Cuerpo</p>",
                "<h1>Después</h1><p>Cuerpo</p>");

        assertTrue(result.getDescription().contains("se modificó el título de \"Antes\" a \"Después\""));
        assertEquals("MODIFIED", result.getChangeKind());
        assertEquals("Después", result.getTargetSnippet());
    }

    @Test
    void describeRemovedSection() {
        DocumentContentDiffUtil.DiffResult result = DocumentContentDiffUtil.describe(
                "<div data-section-key=\"intro\" data-section-title=\"Introducción\"><p>Texto viejo</p></div>",
                "");

        assertTrue(result.getDescription().toLowerCase().contains("elimin"));
        assertEquals("REMOVED", result.getChangeKind());
    }
}
