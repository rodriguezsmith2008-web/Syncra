package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ProjectRequestDTO {

    /**
     * Recibe información necesaria para la creación de proyectos 
     */
    private String name;
    private String description;
    private String groupName;
    private LocalDate startDate;
    private LocalDate endDate;

}
