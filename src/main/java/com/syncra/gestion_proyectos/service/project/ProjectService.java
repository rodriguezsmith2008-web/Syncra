package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncra.gestion_proyectos.dto.project.ProjectRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectResponseDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectUpdateDTO;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    //Repositorio utilizado para acceder y gestionar la información de los proyectos almacenados en la base de datos
    private final ProjectRepository projectRepository;

    /**
     * Obtiene todos los proyectos registrados en el sistema
     * @return lista de proyectos
     */
    public List<ProjectResponseDTO> getAll() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Obtiene un proyecto a partir de su id
     * @param id id del proyecto
     * @return información del proyecto solicitado
     */
    public ProjectResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * Crear un nuevo proyecto
     * @param dto datos necesarios para crear un proyecto
     * @param createdBy id del usuario creador
     * @return información del proyecto creado
     */
    @Transactional
    public ProjectResponseDTO create(ProjectRequestDTO dto, Long createdBy) {
        ProjectEntity entity = new ProjectEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setGroupName(dto.getGroupName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCreatedBy(createdBy);
        return toResponse(projectRepository.save(entity));
    }

    /**
     * Actualiza la información de un proyecto existente 
     * @param id
     * @param dto datos a actualizar
     * @return información actualizada del proyecto
     */
    @Transactional
    public ProjectResponseDTO update(Long id, ProjectUpdateDTO dto) {
        ProjectEntity entity = findOrThrow(id);
        if (dto.getName() != null)        entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getGroupName() != null)   entity.setGroupName(dto.getGroupName());
        if (dto.getStatus() != null)      entity.setStatus(dto.getStatus());
        if (dto.getStartDate() != null)   entity.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null)     entity.setEndDate(dto.getEndDate());
        return toResponse(projectRepository.save(entity));
    }

    /**
     * Elimina un proyecto del sistema
     * @param id 
     */
    @Transactional
    public void delete(Long id) {
        projectRepository.delete(findOrThrow(id));
    }

    /**
     * Actualiza el estado de un proyecto
     * @param id
     * @param status estado del proyecto
     * @return información actualizada del proyecto 
     */
    @Transactional
    public ProjectResponseDTO updateStatus(Long id, ProjectStatusEnum status) {
        ProjectEntity entity = findOrThrow(id);
        entity.setStatus(status);
        return toResponse(projectRepository.save(entity));
    }

    /**
     * Busca un proyecto por su id
     * @param id
     * @return entidad del proyecto encontrada
     */
    private ProjectEntity findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }

    /**
     * Convierte una entidad projectentity en un dto de respuesta
     * @param e entidad del proyecto
     * @return dto con la información del proyecto 
     */
    private ProjectResponseDTO toResponse(ProjectEntity e) {
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
        return r;
    }
}