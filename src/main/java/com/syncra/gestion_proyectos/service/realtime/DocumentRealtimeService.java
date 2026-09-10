package com.syncra.gestion_proyectos.service.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.document.DocumentResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishUpdated(DocumentResponseDTO document) {
        messagingTemplate.convertAndSend(
                "/topic/projects/" + document.getProjectId() + "/documents",
                document);
    }
}