package com.syncra.gestion_proyectos.service.dashboard;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.dashboard.DashboardResponseDTO;
import com.syncra.gestion_proyectos.enums.AccessStatusEnum;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
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

        return dto;
    }

}