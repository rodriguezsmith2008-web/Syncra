package com.syncra.gestion_proyectos.controller.task;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.service.task.TaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    /**
     * Obtiene todas las tareas de un proyecto (tablero kanban completo)
     *
     * @param projectId
     * @return lista de tareas del proyecto
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(projectId));
    }

    /**
     * Obtiene una tarea puntual por su id
     *
     * @param projectId
     * @param taskId
     * @return tarea encontrada
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> getById(@PathVariable Long projectId, @PathVariable Long taskId) {
        return ResponseEntity.ok(service.getById(taskId));
    }

    /**
     * Obtiene las tareas de una columna especifica del proyecto
     *
     * @param projectId
     * @param columnId
     * @return lista de tareas de la columna
     */
    @GetMapping("/column/{columnId}")
    public ResponseEntity<List<TaskResponseDTO>> getByColumn(@PathVariable Long projectId,
            @PathVariable Long columnId) {
        return ResponseEntity.ok(service.getByColumn(columnId));
    }

    /**
     * Obtiene las tareas de un sprint especifico del proyecto
     *
     * @param projectId
     * @param sprintId
     * @return lista de tareas del sprint
     */
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<List<TaskResponseDTO>> getBySprint(@PathVariable Long projectId,
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(service.getBySprint(sprintId));
    }

    /**
     * Obtiene las tareas del proyecto asignadas a un usuario especifico
     *
     * @param projectId
     * @param userId
     * @return lista de tareas asignadas al usuario
     */
    @GetMapping("/assigned/{userId}")
    public ResponseEntity<List<TaskResponseDTO>> getByAssignedUser(@PathVariable Long projectId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(service.getByAssignedUser(projectId, userId));
    }
}