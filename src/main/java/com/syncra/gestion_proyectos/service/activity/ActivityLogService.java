package com.syncra.gestion_proyectos.service.activity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.activity.ActivityLogResponseDTO;
import com.syncra.gestion_proyectos.entity.activity.ActivityLogEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;
import com.syncra.gestion_proyectos.repository.activity.ActivityLogRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final UsersRepository usersRepository;
    private final ActivityLogRealtimeService activityLogRealtimeService;

    /**
     * Registra un evento de actividad (crear/editar/borrar) sobre un
     * documento, recurso, archivo o columna del tablero.
     *
     * @param projectId   proyecto donde ocurrió
     * @param entityType  tipo de entidad afectada
     * @param entityId    id de esa entidad
     * @param action      qué se hizo (CREATED, UPDATED, DELETED)
     * @param description texto ya listo para mostrar en el historial
     * @param userId      quién lo hizo
     */
    public void log(Long projectId, ActivityEntityTypeEnum entityType, Long entityId,
            ActivityActionEnum action, String description, Long userId) {

        ActivityLogEntity entity = new ActivityLogEntity();
        entity.setProjectId(projectId);
        entity.setEntityType(entityType);
        entity.setEntityId(entityId);
        entity.setAction(action);
        entity.setDescription(description);
        entity.setUserId(userId);

        ActivityLogEntity saved = repository.save(entity);
        ActivityLogResponseDTO response = toResponseDTO(saved, usersRepository.findById(userId).orElse(null));
        activityLogRealtimeService.publishChanged(projectId, response);
    }

    /**
     * Obtiene el historial de actividad de un proyecto, más reciente primero
     *
     * @param projectId
     * @return lista de eventos
     */
    public List<ActivityLogResponseDTO> getByProject(Long projectId) {
        return toResponseList(repository.findByProjectIdOrderByCreatedAtDesc(projectId));
    }

            /** Obtiene únicamente los eventos asociados a un documento. */
            public List<ActivityLogResponseDTO> getByDocument(Long projectId, Long documentId) {
                return toResponseList(repository.findByProjectIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    projectId, ActivityEntityTypeEnum.DOCUMENT, documentId));
            }

            private List<ActivityLogResponseDTO> toResponseList(List<ActivityLogEntity> events) {
            Map<Long, UsersEntity> usersById = usersRepository.findAllById(
                events.stream().map(ActivityLogEntity::getUserId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(UsersEntity::getId, Function.identity()));

            return events.stream()
                .map(event -> toResponseDTO(event, usersById.get(event.getUserId())))
                .collect(Collectors.toList());
            }

            private ActivityLogResponseDTO toResponseDTO(ActivityLogEntity entity, UsersEntity user) {
        ActivityLogResponseDTO dto = new ActivityLogResponseDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setAction(entity.getAction());
        dto.setDescription(entity.getDescription());
        dto.setUserId(entity.getUserId());
        if (user != null) {
            dto.setUserFullName((user.getFirstName() + " " + user.getLastName()).trim());
            dto.setUserAvatarUrl(user.getAvatarUrl());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

}