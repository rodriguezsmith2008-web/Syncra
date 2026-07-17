package com.syncra.gestion_proyectos.service.notification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.notifications.NotificationResponseDTO;
import com.syncra.gestion_proyectos.entity.notification.NotificationEntity;
import com.syncra.gestion_proyectos.repository.notification.NotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /**
     * Obtiene todas las notificaciones de un usuario
     *
     * @param userId
     * @return lista de notificaciones
     */
    public List<NotificationResponseDTO> getByUser(Long userId) {

        List<NotificationEntity> notifications = repository.findByUserIdOrderByCreatedAtDesc(userId);
        List<NotificationResponseDTO> response = new ArrayList<>();

        for (NotificationEntity notification : notifications) {

            NotificationResponseDTO dto = new NotificationResponseDTO();

            dto.setId(notification.getId());
            dto.setUserId(notification.getUserId());
            dto.setTaskId(notification.getTaskId());
            dto.setType(notification.getType());
            dto.setMessage(notification.getMessage());
            dto.setIsRead(notification.getIsRead());
            dto.setCreatedAt(notification.getCreatedAt());

            response.add(dto);
        }

        return response;
    }

    /**
     * Obtiene solo las notificaciones no leidas de un usuario
     *
     * @param userId
     * @return lista de notificaciones no leidas
     */
    public List<NotificationResponseDTO> getUnreadByUser(Long userId) {

        List<NotificationEntity> notifications = repository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        List<NotificationResponseDTO> response = new ArrayList<>();

        for (NotificationEntity notification : notifications) {

            NotificationResponseDTO dto = new NotificationResponseDTO();

            dto.setId(notification.getId());
            dto.setUserId(notification.getUserId());
            dto.setTaskId(notification.getTaskId());
            dto.setType(notification.getType());
            dto.setMessage(notification.getMessage());
            dto.setIsRead(notification.getIsRead());
            dto.setCreatedAt(notification.getCreatedAt());

            response.add(dto);
        }

        return response;
    }

    /**
     * Marca una notificacion como leida
     *
     * @param notificationId
     * @param userId id del usuario autenticado
     * @return notificacion actualizada
     */
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
        dto.setTaskId(entity.getTaskId());
        dto.setType(entity.getType());
        dto.setMessage(entity.getMessage());
        dto.setIsRead(entity.getIsRead());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }

    /**
     * Crea una notificacion para un usuario.
     *
     * @param userId destinatario de la notificacion
     * @param taskId tarea relacionada
     * @param type tipo de evento
     * @param message mensaje a mostrar al usuario
     */
    @Transactional
    public void crear(Long userId, Long taskId, String type, String message) {

        NotificationEntity entity = new NotificationEntity();
        entity.setUserId(userId);
        entity.setTaskId(taskId);
        entity.setType(type);
        entity.setMessage(message);
        entity.setIsRead(false);

        repository.save(entity);
    }
}