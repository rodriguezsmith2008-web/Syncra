package com.syncra.gestion_proyectos.dto.project;

import lombok.Data;

@Data
public class InstructorStatsResponseDTO {

    private long totalProjects;
    private long inProgressProjects;
    private long inReviewProjects;
    private long approvedProjects;
    private long rejectedProjects;
    private long totalApprentices;
}