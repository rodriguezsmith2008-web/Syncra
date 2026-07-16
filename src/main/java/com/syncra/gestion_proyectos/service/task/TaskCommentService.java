package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskCommentResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskCommentEntity;
import com.syncra.gestion_proyectos.repository.task.TaskCommentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentRepository repository;

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

        return toResponse(entity);
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