package com.syncra.gestion_proyectos.service.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.dashboard.DashboardResponseDTO;
import com.syncra.gestion_proyectos.dto.dashboard.ProjectStatusCountDTO;
import com.syncra.gestion_proyectos.dto.dashboard.UserRoleCountDTO;
import com.syncra.gestion_proyectos.enums.AccessStatusEnum;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.access.AcessRequestRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UsersRepository usersRepository;
    private final ProjectRepository projectRepository;
    private final AcessRequestRepository accessRepository;

    public DashboardResponseDTO getDashboard() {

        DashboardResponseDTO dto = new DashboardResponseDTO();

        // Total de usuarios
        dto.setTotalUsers(usersRepository.count());

        // Usuarios activos
        dto.setActiveUsers(usersRepository.countByStatus(UserStatusEnum.ACTIVE));

        // Total de proyectos
        dto.setTotalProjects(projectRepository.count());

        // Proyectos en progreso
        dto.setActiveProjects(projectRepository.countByStatus(ProjectStatusEnum.IN_PROGRESS));

        // Solicitudes pendientes
        dto.setPendingAccessRequests(accessRepository.countAllByStatus(AccessStatusEnum.PENDING));

        // Conteo por rol
        List<UserRoleCountDTO> usersByRole = new ArrayList<>();
        usersByRole.add(new UserRoleCountDTO("ADMIN", usersRepository.countByRole(RoleUserEnum.ADMIN)));
        usersByRole.add(new UserRoleCountDTO("INSTRUCTOR", usersRepository.countByRole(RoleUserEnum.INSTRUCTOR)));
        usersByRole.add(new UserRoleCountDTO("APPRENTICE", usersRepository.countByRole(RoleUserEnum.APPRENTICE)));
        dto.setUsersByRole(usersByRole);

        // Conteo por estado de proyecto
        List<ProjectStatusCountDTO> projectsByStatus = new ArrayList<>();
        projectsByStatus.add(new ProjectStatusCountDTO("IN_PROGRESS", projectRepository.countByStatus(ProjectStatusEnum.IN_PROGRESS)));
        projectsByStatus.add(new ProjectStatusCountDTO("IN_REVIEW", projectRepository.countByStatus(ProjectStatusEnum.IN_REVIEW)));
        projectsByStatus.add(new ProjectStatusCountDTO("APPROVED", projectRepository.countByStatus(ProjectStatusEnum.APPROVED)));
        projectsByStatus.add(new ProjectStatusCountDTO("REJECTED", projectRepository.countByStatus(ProjectStatusEnum.REJECTED)));
        dto.setProjectsByStatus(projectsByStatus);

        return dto;
    }

}