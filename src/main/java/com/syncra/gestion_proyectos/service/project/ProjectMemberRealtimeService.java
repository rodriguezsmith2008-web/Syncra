package com.syncra.gestion_proyectos.service.project;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.project.ProjectMemberRealtimeEventDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectMemberResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishChanged(Long projectId, Long userId, String action, ProjectMemberResponseDTO member) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId + "/members",
                new ProjectMemberRealtimeEventDTO(projectId, userId, action, member));

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
