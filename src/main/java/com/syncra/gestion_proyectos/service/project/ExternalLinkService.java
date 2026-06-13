package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ExternalLinkRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ExternalLinkResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ExternalLinkEntity;
import com.syncra.gestion_proyectos.repository.project.ExternalLinkRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalLinkService {

    private final ExternalLinkRepository linkRepository;

    public List<ExternalLinkResponseDTO> getByProject(Long projectId) {
        return linkRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ExternalLinkResponseDTO getById(Long projectId, Long linkId) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        return toResponse(entity);
    }

    @Transactional
    public ExternalLinkResponseDTO create(Long projectId, ExternalLinkRequestDTO dto, Long addedBy) {
        ExternalLinkEntity entity = new ExternalLinkEntity();
        entity.setProjectId(projectId);
        entity.setTitle(dto.getTitle());
        entity.setUrl(dto.getUrl());
        entity.setAddedBy(addedBy);
        return toResponse(linkRepository.save(entity));
    }

    @Transactional
    public ExternalLinkResponseDTO update(Long projectId, Long linkId, ExternalLinkRequestDTO dto) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        if (dto.getTitle() != null)
            entity.setTitle(dto.getTitle());
        if (dto.getUrl() != null)
            entity.setUrl(dto.getUrl());
        return toResponse(linkRepository.save(entity));
    }

    @Transactional
    public void delete(Long projectId, Long linkId) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        linkRepository.delete(entity);
    }

    private ExternalLinkEntity findOrThrow(Long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ExternalLink not found: " + id));
    }

    private void validateBelongsToProject(ExternalLinkEntity entity, Long projectId) {
        if (!entity.getProjectId().equals(projectId)) {
            throw new EntityNotFoundException("El enlace no pertenece al proyecto: " + projectId);
        }
    }

    private ExternalLinkResponseDTO toResponse(ExternalLinkEntity e) {
        ExternalLinkResponseDTO r = new ExternalLinkResponseDTO();
        r.setId(e.getId());
        r.setProjectId(e.getProjectId());
        r.setTitle(e.getTitle());
        r.setUrl(e.getUrl());
        r.setAddedBy(e.getAddedBy());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
