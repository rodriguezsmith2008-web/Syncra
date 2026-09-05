package com.syncra.gestion_proyectos.dto.document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocTemplateCreateDTO {

    @NotBlank
    private String title;

    private String description;

    private String defaultContent;
}