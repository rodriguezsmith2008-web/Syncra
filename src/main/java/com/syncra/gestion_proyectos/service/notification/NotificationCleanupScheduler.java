package com.syncra.gestion_proyectos.service.notification;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncra.gestion_proyectos.repository.notification.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCleanupScheduler {

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "America/Bogota")
    public void deleteOldReadNotifications() {
        LocalDateTime limite = LocalDateTime.now().minusDays(10);
        long deletedCount = notificationRepository.deleteByIsReadTrueAndCreatedAtBefore(limite);
        log.info("Se eliminaron {} notificaciones leidas anteriores a {}", deletedCount, limite);
    }
}