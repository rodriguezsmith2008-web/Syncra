package com.syncra.gestion_proyectos.service.sprint;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.sprint.SprintRealtimeEventDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishChanged(Long projectId, Long sprintId, String type) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId + "/sprints",
                new SprintRealtimeEventDTO(projectId, sprintId, type));

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
