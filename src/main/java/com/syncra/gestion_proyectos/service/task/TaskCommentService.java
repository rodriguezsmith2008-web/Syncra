package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskCommentResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskCommentEntity;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.repository.task.TaskCommentRepository;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import jakarta.transaction.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository repository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final TaskRealtimeService taskRealtimeService;

    /**
     * Obtiene todos los comentarios de una tarea
     *
     * @param taskId
     * @return lista de comentarios
     */
    public List<TaskCommentResponseDTO> getByTask(Long taskId) {

        List<TaskCommentEntity> comments = repository.findByTaskIdOrderByCreatedAtAsc(taskId);
        List<TaskCommentResponseDTO> response = new ArrayList<>();

        for (TaskCommentEntity comment : comments) {
            response.add(toResponse(comment));
        }

        return response;
    }

    /**
     * Agrega un comentario a una tarea
     *
     * @param taskId
     * @param userId id del usuario autenticado que comenta
     * @param dto
     * @return comentario creado
     */
    @Transactional
    public TaskCommentResponseDTO addComment(Long taskId, Long userId, TaskCommentRequestDTO dto) {

        TaskCommentEntity entity = new TaskCommentEntity();
        entity.setTaskId(taskId);
        entity.setUserId(userId);
        entity.setContent(dto.getContent());

        repository.save(entity);

        TaskEntity task = taskRepository.findById(taskId).orElse(null);
        if (task != null && task.getAssignedTo() != null && !task.getAssignedTo().equals(userId)) {
            notificationService.crear(
                task.getAssignedTo(),
                userId,
                task.getProjectId(),
                taskId,
                null,
                entity.getId(),
                "TASK_COMMENT",
                "Nuevo comentario en la tarea: " + task.getTitle());
        }

        TaskCommentResponseDTO response = toResponse(entity);
        if (task != null) {
            taskRealtimeService.publishCommentChanged(task.getProjectId(), taskId, entity.getId(), "CREATED", response);
        }

        return response;
    }

    @Transactional
    public TaskCommentResponseDTO updateComment(Long taskId, Long commentId, Long userId, TaskCommentRequestDTO dto) {
        TaskCommentEntity entity = findOwnedComment(taskId, commentId, userId);
        entity.setContent(dto.getContent());
        TaskCommentResponseDTO response = toResponse(repository.save(entity));

        taskRepository.findById(taskId).ifPresent(task ->
                taskRealtimeService.publishCommentChanged(task.getProjectId(), taskId, commentId, "UPDATED", response));

        return response;
    }

    @Transactional
    public void deleteComment(Long taskId, Long commentId, Long userId) {
        repository.delete(findOwnedComment(taskId, commentId, userId));

        taskRepository.findById(taskId).ifPresent(task ->
                taskRealtimeService.publishCommentChanged(task.getProjectId(), taskId, commentId, "DELETED", null));
    }

    private TaskCommentEntity findOwnedComment(Long taskId, Long commentId, Long userId) {
        TaskCommentEntity entity = repository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comentario no encontrado"));
        if (!entity.getTaskId().equals(taskId) || !entity.getUserId().equals(userId)) {
            throw new EntityNotFoundException("Comentario no encontrado");
        }
        return entity;
    }

    /**
     * Convierte una entidad en su dto de respuesta
     *
     * @param entity
     * @return dto con la informacion del comentario
     */
    private TaskCommentResponseDTO toResponse(TaskCommentEntity entity) {

        TaskCommentResponseDTO dto = new TaskCommentResponseDTO();

        dto.setId(entity.getId());
        dto.setTaskId(entity.getTaskId());
        dto.setUserId(entity.getUserId());
        dto.setContent(entity.getContent());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}