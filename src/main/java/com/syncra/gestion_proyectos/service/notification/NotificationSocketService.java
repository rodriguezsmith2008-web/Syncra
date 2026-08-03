package com.syncra.gestion_proyectos.service.notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.notifications.NotificationResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long userId, NotificationResponseDTO dto) {

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + userId,
                dto
        );
    }
}