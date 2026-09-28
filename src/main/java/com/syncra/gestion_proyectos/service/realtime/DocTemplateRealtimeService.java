package com.syncra.gestion_proyectos.service.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.document.DocTemplateRealtimeEventDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocTemplateRealtimeService {

    private static final String TOPIC = "/topic/doc-templates";

    private final SimpMessagingTemplate messagingTemplate;

    public void publishChanged(Long templateId, String type) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                TOPIC,
                new DocTemplateRealtimeEventDTO(templateId, type));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
            return;
        }

        publish.run();
    }
}