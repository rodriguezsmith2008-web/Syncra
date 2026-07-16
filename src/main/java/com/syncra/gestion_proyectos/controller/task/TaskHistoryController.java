package com.syncra.gestion_proyectos.controller.task;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.task.TaskHistoryResponseDTO;
import com.syncra.gestion_proyectos.service.task.TaskHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/tasks/{taskId}/history")
@RequiredArgsConstructor
public class TaskHistoryController {

    private final TaskHistoryService service;

    /**
     * Obtiene el historial de cambios de una tarea (solo lectura)
     *
     * @param projectId
     * @param taskId
     * @return lista de registros de historial
     */
    @GetMapping
    public ResponseEntity<List<TaskHistoryResponseDTO>> getByTask(@PathVariable Long projectId,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTask(taskId));
    }
}