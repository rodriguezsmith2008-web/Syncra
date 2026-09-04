package com.syncra.gestion_proyectos.service.project;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncra.gestion_proyectos.dto.project.ProjectRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectResponseDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectUpdateDTO;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UsersRepository userRepository;
    private final ProjectMemberService projectMemberService;
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    /**
     * Obtiene todos los proyectos registrados en el sistema
     * 
     * @return lista de proyectos
     */
    public List<ProjectResponseDTO> getAll() {
        return toResponseList(projectRepository.findAll());
    }

    /**
     * Crear un nuevo proyecto
     * 
     * @param dto       datos necesarios para crear un proyecto
     * @param createdBy id del usuario creador
     * @return información del proyecto creado
     */
    public ProjectResponseDTO getById(Long id) {

        ProjectEntity entity = findOrThrow(id);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(entity.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(entity, creators);
    }

    public List<ProjectResponseDTO> getByStatus(ProjectStatusEnum status) {
        return toResponseList(projectRepository.findByStatus(status));
    }

    @Transactional
    public ProjectResponseDTO create(ProjectRequestDTO dto, Long createdBy) {

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getStartDate().isAfter(dto.getEndDate())) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
        }

        ProjectEntity entity = new ProjectEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setGroupName(dto.getGroupName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCreatedBy(createdBy);

        ProjectEntity saved = projectRepository.save(entity);

        try {
            projectMemberService.addMember(saved.getId(), createdBy);
        } catch (Exception e) {
            log.warn("No se pudo agregar al creador como miembro: {}", e.getMessage());
        }

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(createdBy)).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }
 /**
     * Actualiza la información de un proyecto existente
     * 
     * @param id
     * @param dto datos a actualizar
     * @return información actualizada del proyecto
     */
    @Transactional
    public ProjectResponseDTO update(Long id, ProjectUpdateDTO dto) {
        ProjectEntity entity = findOrThrow(id);
        if (dto.getName() != null)
            entity.setName(dto.getName());
        if (dto.getDescription() != null)
            entity.setDescription(dto.getDescription());
        if (dto.getGroupName() != null)
            entity.setGroupName(dto.getGroupName());
        if (dto.getStatus() != null)
            entity.setStatus(dto.getStatus());
        if (dto.getStartDate() != null)
            entity.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null)
            entity.setEndDate(dto.getEndDate());

        ProjectEntity saved = projectRepository.save(entity);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(saved.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(findOrThrow(id));
    }
    /**
     * Actualiza el estado de un proyecto
     * 
     * @param id
     * @param status estado del proyecto
     * @return información actualizada del proyecto
     */
    @Transactional
    public ProjectResponseDTO updateStatus(Long id, ProjectStatusEnum status) {
        ProjectEntity entity = findOrThrow(id);
        entity.setStatus(status);
        ProjectEntity saved = projectRepository.save(entity);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(saved.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }

    private ProjectEntity findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }

    private List<ProjectResponseDTO> toResponseList(List<ProjectEntity> projects) {

        List<Long> creatorIds = projects.stream()
                .map(ProjectEntity::getCreatedBy)
                .distinct()
                .toList();

        Map<Long, UsersEntity> creatorsById = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return projects.stream()
                .map(p -> toResponse(p, creatorsById))
                .toList();
    }

    private ProjectResponseDTO toResponse(ProjectEntity e, Map<Long, UsersEntity> creatorsById) {
        ProjectResponseDTO r = new ProjectResponseDTO();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setDescription(e.getDescription());
        r.setGroupName(e.getGroupName());
        r.setStatus(e.getStatus());
        r.setStartDate(e.getStartDate());
        r.setEndDate(e.getEndDate());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        UsersEntity creator = creatorsById.get(e.getCreatedBy());
        r.setCreatedByName(creator != null
                ? creator.getFirstName() + " " + creator.getLastName()
                : "Usuario desconocido");

        return r;
    }

    public List<ProjectResponseDTO> getMine(Long userId) {

        List<ProjectEntity> projects = projectMemberRepository.findByIdUserId(userId).stream()
                .map(member -> projectRepository.findById(member.getId().getProjectId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        return toResponseList(projects);
    }
}