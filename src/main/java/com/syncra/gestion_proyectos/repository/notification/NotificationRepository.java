package com.syncra.gestion_proyectos.repository.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.notification.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Obtiene todas las notificaciones de un usuario, de la mas reciente a la mas antigua
     *
     * @param userId
     * @return lista de notificaciones
     */
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Obtiene las notificaciones no leidas de un usuario
     *
     * @param userId
     * @return lista de notificaciones no leidas
     */
    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);
}