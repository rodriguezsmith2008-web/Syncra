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

    private final ProjectRepository projectRepository;

    public List<ProjectResponseDTO> getAll() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponseDTO getById(Long id) {
        return toResponse(findOrThrow(id));
    }

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

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(findOrThrow(id));
    }

    @Transactional
    public ProjectResponseDTO updateStatus(Long id, ProjectStatusEnum status) {
        ProjectEntity entity = findOrThrow(id);
        entity.setStatus(status);
        return toResponse(projectRepository.save(entity));
    }

    private ProjectEntity findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }

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