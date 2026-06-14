package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDate;

import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

import lombok.Data;

@Data
public class ProjectUpdateDTO {

    /**
     * Recibe información necesaria para actualizar un proyecto y permite modificar los datos básicos
     */
    private String name;
    private String description;
    private String groupName;
    private ProjectStatusEnum status;
    private LocalDate startDate;
    private LocalDate endDate;

}