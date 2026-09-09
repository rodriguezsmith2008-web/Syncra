package com.syncra.gestion_proyectos.repository.notification;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.syncra.gestion_proyectos.entity.notification.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Obtiene todas las notificaciones de un usuario, de la mas reciente a la mas
     * antigua
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

    /**
     * contea las notificaciones no leidas
     * 
     * @param userId
     * @return
     */
    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserIdAndTaskIdAndType(Long userId, Long taskId, String type);

    /**
     * Metodo para leer toda las notificaciones
     * 
     * @param userId
     */
    @Modifying
    @Query("""
            UPDATE NotificationEntity n
                    SET n.isRead = true
                    WHERE n.userId = :userId
                    AND n.isRead = false
                    """)
    void markAllAsRead(Long userId);

    /**
     *  borra  todas las notificaciones leidas
     * @param id
     * @param userId
     */
    void deleteByIdAndUserId(Long id, Long userId);

    /**
     * Borra una sola notificacion
     * @param userId
     */
    void deleteByUserIdAndIsReadTrue(Long userId);
}