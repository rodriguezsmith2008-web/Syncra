package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

import lombok.Data;

@Data
public class ProjectResponseDTO {

    /**
     * Dto de respuesta para devolver la información de un proyecto solo contiene los datos principales de un proyecto
     */
    private Long id;
    private String name;
    private String description;
    private String groupName;
    private ProjectStatusEnum status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private long totalDocuments;
    private long approvedDocuments;
    private int progress;
}
