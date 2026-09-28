package com.syncra.gestion_proyectos.controller.task;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.task.TaskHistoryResponseDTO;
import com.syncra.gestion_proyectos.service.task.TaskHistoryService;
import com.syncra.gestion_proyectos.service.task.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskHistoryController {

    private final TaskHistoryService service;
    private final TaskService taskService;

    /**
     * Obtiene el historial de cambios de una tarea (solo lectura)
     *
     * @param projectId
     * @param taskId
     * @return lista de registros de historial
     */
    @GetMapping("/{taskId}/history")
    public ResponseEntity<List<TaskHistoryResponseDTO>> getByTask(@PathVariable Long projectId,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getByTask(taskId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TaskHistoryResponseDTO>> getByProject(@PathVariable Long projectId) {
        List<Long> taskIds = taskService.getByProject(projectId).stream()
                .map(task -> task.getId())
                .collect(Collectors.toList());
        return ResponseEntity.ok(service.getByTasks(taskIds));
    }
}