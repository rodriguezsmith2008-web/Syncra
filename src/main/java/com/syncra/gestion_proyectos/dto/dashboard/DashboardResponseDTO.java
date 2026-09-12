package com.syncra.gestion_proyectos.dto.dashboard;

import java.util.List;

import lombok.Data;

@Data
public class DashboardResponseDTO {

    private Long totalUsers;

    private Long activeUsers;

    private Long totalProjects;

    private Long activeProjects;

    private Long pendingAccessRequests;

    private List<UserRoleCountDTO> usersByRole;

    private List<ProjectStatusCountDTO> projectsByStatus;

}