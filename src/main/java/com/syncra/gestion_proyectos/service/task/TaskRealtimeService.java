package com.syncra.gestion_proyectos.service.task;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.task.TaskRealtimeEventDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishChanged(Long projectId, Long taskId, String type) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId + "/tasks",
                new TaskRealtimeEventDTO(projectId, taskId, type));

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
