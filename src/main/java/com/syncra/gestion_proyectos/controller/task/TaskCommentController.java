package com.syncra.gestion_proyectos.controller.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.task.TaskCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskCommentResponseDTO;
import com.syncra.gestion_proyectos.service.task.TaskCommentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class TaskCommentController {

    private final TaskCommentService service;

    /**
     * Obtiene todos los comentarios de una tarea
     *
     * @param projectId
     * @param taskId
     * @return lista de comentarios
     */
    @GetMapping
    public ResponseEntity<List<TaskCommentResponseDTO>> getByTask(@PathVariable Long projectId,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTask(taskId));
    }

    /**
     * Agrega un comentario a una tarea
     *
     * @param projectId
     * @param taskId
     * @param dto
     * @param request usado para obtener el id del usuario autenticado
     * @return comentario creado
     */
    @PostMapping
    public ResponseEntity<TaskCommentResponseDTO> addComment(@PathVariable Long projectId, @PathVariable Long taskId,
            @RequestBody TaskCommentRequestDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addComment(taskId, userId, dto));
    }
}