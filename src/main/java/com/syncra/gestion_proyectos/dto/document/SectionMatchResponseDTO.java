package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

@Data
public class SectionMatchResponseDTO {

    private String sectionKey;
    private String sectionTitle;
    private Long sourceDocumentId;
    private String sourceDocumentTitle;
    private String content;
}