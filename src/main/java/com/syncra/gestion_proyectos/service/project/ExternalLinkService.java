package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ExternalLinkRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ExternalLinkResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ExternalLinkEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;
import com.syncra.gestion_proyectos.repository.project.ExternalLinkRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalLinkService {

    private final ExternalLinkRepository linkRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

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

        ExternalLinkEntity saved = linkRepository.save(entity);

        notificarMiembros(projectId, addedBy, "RESOURCE_ADDED",
                "Se agregó un nuevo recurso: " + saved.getTitle());

        activityLogService.log(projectId, ActivityEntityTypeEnum.EXTERNAL_LINK, saved.getId(),
                ActivityActionEnum.CREATED, "agregó el recurso \"" + saved.getTitle() + "\"", addedBy);

        return toResponse(saved);
    }

    @Transactional
    public ExternalLinkResponseDTO update(Long projectId, Long linkId, ExternalLinkRequestDTO dto, Long userId) {

        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);

        if (dto.getTitle() != null)
            entity.setTitle(dto.getTitle());
        if (dto.getUrl() != null)
            entity.setUrl(dto.getUrl());

        ExternalLinkEntity saved = linkRepository.save(entity);

        notificarMiembros(projectId, userId, "RESOURCE_UPDATED",
                "Se actualizó el recurso: " + saved.getTitle());

        activityLogService.log(projectId, ActivityEntityTypeEnum.EXTERNAL_LINK, saved.getId(),
                ActivityActionEnum.UPDATED, "editó el recurso \"" + saved.getTitle() + "\"", userId);

        return toResponse(saved);
    }

    @Transactional
    public void delete(Long projectId, Long linkId, Long userId) {

        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);

        String title = entity.getTitle();

        linkRepository.delete(entity);

        notificarMiembros(projectId, userId, "RESOURCE_DELETED",
                "Se eliminó el recurso: " + title);

        activityLogService.log(projectId, ActivityEntityTypeEnum.EXTERNAL_LINK, linkId,
                ActivityActionEnum.DELETED, "eliminó el recurso \"" + title + "\"", userId);
    }

    private void notificarMiembros(Long projectId, Long actorId, String type, String message) {

        List<ProjectMemberEntity> members = projectMemberRepository.findByIdProjectId(projectId);

        for (ProjectMemberEntity member : members) {

            Long memberId = member.getId().getUserId();

            if (!memberId.equals(actorId)) {
                notificationService.crear(memberId, actorId, projectId, null, null, null, type, message);
            }
        }
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