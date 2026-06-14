package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExternalLinkResponseDTO {

    /**
     * Dto utilizado para devolver la información de un enlace externo asociado a un proyecto
     */
    private Long id;
    private Long projectId;
    private String title;
    private String url;
    private Long addedBy;
    private LocalDateTime createdAt;

}
