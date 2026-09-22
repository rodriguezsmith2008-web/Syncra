package com.syncra.gestion_proyectos.dto.document;

import java.util.List;

import lombok.Data;

/** DTO de respuesta con la información de una plantilla de documento */
@Data
public class DocTemplateResponseDTO {

    private Long id;
    private String code;
    private String title;
    private String description;
    private Long position;
    private String defaultContent;
    private boolean published;
    private Long parentTemplateId;
    private List<DocTemplateResponseDTO> children;
    private List<DocTemplateSectionDTO> sections;
}