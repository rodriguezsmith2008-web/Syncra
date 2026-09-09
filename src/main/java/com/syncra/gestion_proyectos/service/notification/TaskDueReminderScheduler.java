package com.syncra.gestion_proyectos.service.notification;

import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.repository.notification.NotificationRepository;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDueReminderScheduler {
    private static final String REMINDER_TYPE = "TASK_DUE_SOON";
    private final TaskRepository tasks;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *")
    public void notifyTasksDueTomorrow() {
        for (TaskEntity task : tasks.findByDueDate(LocalDate.now().plusDays(1))) {
            if (task.getAssignedTo() == null || notifications.existsByUserIdAndTaskIdAndType(
                    task.getAssignedTo(), task.getId(), REMINDER_TYPE)) continue;
            notificationService.crear(task.getAssignedTo(), null, task.getProjectId(), task.getId(), null, null,
                    REMINDER_TYPE, "La tarea '" + task.getTitle() + "' vence mañana");
        }
    }
}