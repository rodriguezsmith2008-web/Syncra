package com.syncra.gestion_proyectos.dto.project;

import java.time.LocalDate;

import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

import lombok.Data;

@Data
public class ProjectUpdateDTO {

    private String name;
    private String description;
    private String groupName;
    private ProjectStatusEnum status;
    private LocalDate startDate;
    private LocalDate endDate;

}