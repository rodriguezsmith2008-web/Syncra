package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ExternalLinkResponseDTO {

    private Long id;
    private Long projectId;
    private String title;
    private String url;
    private Long addedBy;
    private LocalDateTime createdAt;

}
