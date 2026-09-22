package com.syncra.gestion_proyectos.service.notification;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.repository.kanban.KanbanColumnRepository;
import com.syncra.gestion_proyectos.repository.notification.NotificationRepository;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskDueReminderScheduler {
    private static final String REMINDER_TYPE = "TASK_DUE_SOON";
    private static final String REMINDER_24H_TYPE = "TASK_DUE_24H";
    private static final String REMINDER_3H_TYPE = "TASK_DUE_3H";
    private final TaskRepository tasks;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final KanbanColumnRepository columns;

    @Scheduled(cron = "0 0 * * * *")
    public void notifyTasksDueTomorrow() {
        for (TaskEntity task : tasks.findByDueDate(LocalDate.now().plusDays(1))) {
            if (task.getAssignedTo() == null || notifications.existsByUserIdAndTaskIdAndType(
                    task.getAssignedTo(), task.getId(), REMINDER_TYPE)) continue;
            notificationService.crear(task.getAssignedTo(), null, task.getProjectId(), task.getId(), null, null,
                    REMINDER_TYPE, "La tarea '" + task.getTitle() + "' vence mañana");
        }

        notifyTasksByHoursUntilDue();
    }

    private void notifyTasksByHoursUntilDue() {
        LocalDateTime now = LocalDateTime.now();

        for (TaskEntity task : tasks.findAll()) {
            if (task.getAssignedTo() == null || task.getDueDate() == null) {
                continue;
            }

            boolean finalColumn = columns.findById(task.getColumnId())
                    .map(column -> Boolean.TRUE.equals(column.getIsFinal()))
                    .orElse(false);
            if (finalColumn) {
                continue;
            }

            LocalTime dueTime = task.getDueTime() != null
                    ? task.getDueTime()
                    : LocalTime.of(23, 59);
            LocalDateTime dueAt = LocalDateTime.of(task.getDueDate(), dueTime);
            long minutesUntilDue = Duration.between(now, dueAt).toMinutes();

            if (minutesUntilDue >= 23 * 60 && minutesUntilDue <= 24 * 60) {
                sendReminderIfNeeded(task, REMINDER_24H_TYPE,
                        "La tarea '" + task.getTitle() + "' vence en aproximadamente 24 horas");
            }

            if (minutesUntilDue >= 2 * 60 && minutesUntilDue <= 3 * 60) {
                sendReminderIfNeeded(task, REMINDER_3H_TYPE,
                        "La tarea '" + task.getTitle() + "' vence en aproximadamente 3 horas");
            }
        }
    }

    private void sendReminderIfNeeded(TaskEntity task, String type, String message) {
        if (notifications.existsByUserIdAndTaskIdAndType(task.getAssignedTo(), task.getId(), type)) {
            return;
        }

        notificationService.crear(
                task.getAssignedTo(),
                null,
                task.getProjectId(),
                task.getId(),
                null,
                null,
                type,
                message);
    }
}