package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

@Data
public class DocTemplateSectionDTO {

    private Long id;
    private Long templateId;
    private String sectionKey;
    private String title;
    private String defaultContent;
    private Integer position;

}