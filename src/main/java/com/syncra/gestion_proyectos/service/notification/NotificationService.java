package com.syncra.gestion_proyectos.service.notification;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

import com.syncra.gestion_proyectos.dto.notifications.NotificationResponseDTO;
import com.syncra.gestion_proyectos.entity.notification.NotificationEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.repository.notification.NotificationRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.service.email.EmailService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final Set<String> EMAIL_NOTIFICATION_TYPES = Set.of(
            "TASK_DUE_SOON",
            "TASK_DUE_24H",
            "TASK_DUE_3H",
            "DOCUMENT_REJECTED",
            "CHAT_PRIVATE");

    private final NotificationRepository repository;
    private final NotificationSocketService socketService;
    private final UsersRepository usersRepository;
    private final EmailService emailService;

    public List<NotificationResponseDTO> getByUser(Long userId) {

        List<NotificationEntity> notifications = repository.findByUserIdOrderByCreatedAtDesc(userId);
        return toResponseListConActor(notifications);
    }

    public List<NotificationResponseDTO> getUnreadByUser(Long userId) {

        List<NotificationEntity> notifications = repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return toResponseListConActor(notifications);
    }

    private List<NotificationResponseDTO> toResponseListConActor(List<NotificationEntity> notifications) {

        List<Long> actorIds = notifications.stream()
                .map(NotificationEntity::getActorUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UsersEntity> actorsById = usersRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        List<NotificationResponseDTO> response = new ArrayList<>();

        for (NotificationEntity notification : notifications) {

            NotificationResponseDTO dto = new NotificationResponseDTO();

            dto.setId(notification.getId());
            dto.setUserId(notification.getUserId());
            dto.setActorUserId(notification.getActorUserId());
            dto.setProjectId(notification.getProjectId());
            dto.setTaskId(notification.getTaskId());
            dto.setDocumentId(notification.getDocumentId());
            dto.setCommentId(notification.getCommentId());
            dto.setType(notification.getType());
            dto.setMessage(notification.getMessage());
            dto.setIsRead(notification.getIsRead());
            dto.setCreatedAt(notification.getCreatedAt());

            UsersEntity actor = actorsById.get(notification.getActorUserId());

            if (actor != null) {
                dto.setActorFullName(actor.getFirstName() + " " + actor.getLastName());
                dto.setActorAvatarUrl(actor.getAvatarUrl());
            }

            response.add(dto);
        }

        return response;
    }

    @Transactional
    public NotificationResponseDTO markAsRead(Long notificationId, Long userId) {

        NotificationEntity entity = repository.findById(notificationId).orElse(null);

        if (entity == null || !entity.getUserId().equals(userId)) {
            return null;
        }

        entity.setIsRead(true);
        repository.save(entity);

        NotificationResponseDTO dto = new NotificationResponseDTO();

        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setActorUserId(entity.getActorUserId());
        dto.setProjectId(entity.getProjectId());
        dto.setTaskId(entity.getTaskId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setCommentId(entity.getCommentId());
        dto.setType(entity.getType());
        dto.setMessage(entity.getMessage());
        dto.setIsRead(entity.getIsRead());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getActorUserId() != null) {
            usersRepository.findById(entity.getActorUserId()).ifPresent(actor -> {
                dto.setActorFullName(actor.getFirstName() + " " + actor.getLastName());
                dto.setActorAvatarUrl(actor.getAvatarUrl());
            });
        }

        return dto;
    }

    @Transactional
    @Async
    public void crear(Long userId, Long actorUserId, Long projectId, Long taskId, Long documentId, Long commentId,
            String type, String message) {

        NotificationEntity entity = new NotificationEntity();

        entity.setUserId(userId);
        entity.setActorUserId(actorUserId);
        entity.setProjectId(projectId);
        entity.setTaskId(taskId);
        entity.setDocumentId(documentId);
        entity.setCommentId(commentId);
        entity.setType(type);
        entity.setMessage(message);
        entity.setIsRead(false);

        repository.save(entity);

        NotificationResponseDTO dto = new NotificationResponseDTO();

        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setActorUserId(entity.getActorUserId());
        dto.setProjectId(entity.getProjectId());
        dto.setTaskId(entity.getTaskId());
        dto.setDocumentId(entity.getDocumentId());
        dto.setCommentId(entity.getCommentId());
        dto.setType(entity.getType());
        dto.setMessage(entity.getMessage());
        dto.setIsRead(entity.getIsRead());
        dto.setCreatedAt(entity.getCreatedAt());

        if (actorUserId != null) {
            usersRepository.findById(actorUserId).ifPresent(actor -> {
                dto.setActorFullName(actor.getFirstName() + " " + actor.getLastName());
                dto.setActorAvatarUrl(actor.getAvatarUrl());
            });
        }

        socketService.sendToUser(userId, dto);

        sendEmailIfRequired(userId, type, message);
    }

    private void sendEmailIfRequired(Long userId, String type, String notificationMessage) {
        if (!EMAIL_NOTIFICATION_TYPES.contains(type)) {
            return;
        }

        try {
            usersRepository.findById(userId)
                    .filter(user -> user.getEmail() != null && !user.getEmail().isBlank())
                    .ifPresent(user -> emailService.sendNotificationEmail(
                            user.getEmail(),
                            type,
                            notificationMessage));
        } catch (Exception e) {
            log.error("No se pudo preparar el correo de notificacion {} para el usuario {}: {}",
                    type, userId, e.getMessage(), e);
        }
    }

    public long countUnread(Long userId) {
        return repository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void marcarTodasComoLeidas(Long userId) {
        repository.markAllAsRead(userId);
    }

    @Transactional
    public void eliminar(Long id, Long userId) {
        repository.deleteByIdAndUserId(id, userId);
    }

    @Transactional
    public void eliminarLeidas(Long userId) {
        repository.deleteByUserIdAndIsReadTrue(userId);
    }
}