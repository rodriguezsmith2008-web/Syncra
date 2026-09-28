package com.syncra.gestion_proyectos.service.kanban;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbanColumnRealtimeEventDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KanbanColumnRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishChanged(Long projectId, Long columnId, String type) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId + "/kanban-columns",
                new KanbanColumnRealtimeEventDTO(projectId, columnId, type));

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
