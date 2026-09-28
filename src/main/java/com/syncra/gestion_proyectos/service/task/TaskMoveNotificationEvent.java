package com.syncra.gestion_proyectos.service.task;

public record TaskMoveNotificationEvent(
        Long taskId,
        Long projectId,
        String taskTitle,
        Long recipientUserId,
        Long actorUserId
) {
}
