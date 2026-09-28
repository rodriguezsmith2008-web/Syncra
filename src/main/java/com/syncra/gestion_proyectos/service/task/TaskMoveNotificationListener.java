package com.syncra.gestion_proyectos.service.task;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.syncra.gestion_proyectos.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TaskMoveNotificationListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskMoved(TaskMoveNotificationEvent event) {
        notificationService.crear(
                event.recipientUserId(),
                event.actorUserId(),
                event.projectId(),
                event.taskId(),
                null,
                null,
                "TASK_MOVED",
                "La tarea '" + event.taskTitle() + "' cambió de posición en el tablero"
        );
    }
}
