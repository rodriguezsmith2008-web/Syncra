package com.syncra.gestion_proyectos.controller.task;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.task.TaskMoveDTO;
import com.syncra.gestion_proyectos.dto.task.TaskRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.task.TaskService;

import jakarta.servlet.http.HttpServletRequest;
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

    /**
     * Crea una nueva tarea en el proyecto
     *
     * @param projectId
     * @param dto
     * @param request usado para obtener el id del usuario autenticado
     * @return tarea creada
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(@PathVariable Long projectId, @RequestBody TaskRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(projectId, userId, dto));
    }

    /**
     * Actualiza los datos de una tarea (no cambia columna ni posicion)
     *
     * @param projectId
     * @param taskId
     * @param dto
     * @param request usado para obtener el id del usuario autenticado
     * @return tarea actualizada
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{taskId}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long projectId, @PathVariable Long taskId,
            @RequestBody TaskRequestDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(service.update(taskId, userId, dto));
    }

    /**
     * Mueve una tarea a otra columna y/o posicion (drag & drop del tablero)
     *
     * @param projectId
     * @param taskId
     * @param dto
     * @param request usado para obtener el id del usuario autenticado
     * @return tarea movida
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{taskId}/move")
    public ResponseEntity<TaskResponseDTO> move(@PathVariable Long projectId, @PathVariable Long taskId,
            @RequestBody TaskMoveDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(service.move(taskId, userId, dto));
    }

    /**
     * Elimina una tarea
     *
     * @param projectId
     * @param taskId
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long taskId) {
        service.delete(taskId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{taskId}/unassign")
    public ResponseEntity<TaskResponseDTO> unassign(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(service.unassign(taskId, userId));
    }
}