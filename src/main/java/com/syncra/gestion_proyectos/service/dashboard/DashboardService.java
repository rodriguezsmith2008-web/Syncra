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
        dto.setActiveUsers(
                (long) usersRepository.findByStatus(UserStatusEnum.ACTIVE).size());

        // Total de proyectos
        dto.setTotalProjects(projectRepository.count());

        // Proyectos en progreso
        dto.setActiveProjects(
                (long) projectRepository.findByStatus(ProjectStatusEnum.IN_PROGRESS).size());

        // Solicitudes pendientes
        dto.setPendingAccessRequests(
                (long) accessRepository.findAllByStatus(AccessStatusEnum.PENDING).size());

        // Conteo por rol
        List<UserRoleCountDTO> usersByRole = new ArrayList<>();
        usersByRole.add(new UserRoleCountDTO("ADMIN", (long) usersRepository.findByRole(RoleUserEnum.ADMIN).size()));
        usersByRole.add(new UserRoleCountDTO("INSTRUCTOR", (long) usersRepository.findByRole(RoleUserEnum.INSTRUCTOR).size()));
        usersByRole.add(new UserRoleCountDTO("APPRENTICE", (long) usersRepository.findByRole(RoleUserEnum.APPRENTICE).size()));
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