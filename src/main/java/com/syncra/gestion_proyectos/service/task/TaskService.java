package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskMoveDTO;
import com.syncra.gestion_proyectos.dto.task.TaskRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;

    /**
     * Obtiene todas las tareas de un proyecto (tablero kanban completo)
     *
     * @param projectId
     * @return lista de tareas del proyecto
     */
    public List<TaskResponseDTO> getByProject(Long projectId) {
        return toResponseList(repository.findByProjectIdOrderByColumnIdAscPositionAsc(projectId));
    }

    /**
     * Obtiene las tareas de una columna especifica
     *
     * @param columnId
     * @return lista de tareas de la columna
     */
    public List<TaskResponseDTO> getByColumn(Long columnId) {
        return toResponseList(repository.findByColumnIdOrderByPositionAsc(columnId));
    }

    /**
     * Obtiene las tareas de un sprint especifico
     *
     * @param sprintId
     * @return lista de tareas del sprint
     */
    public List<TaskResponseDTO> getBySprint(Long sprintId) {
        return toResponseList(repository.findBySprintId(sprintId));
    }

    /**
     * Obtiene las tareas de un proyecto asignadas a un usuario (vista "mis tareas")
     *
     * @param projectId
     * @param assignedTo
     * @return lista de tareas asignadas
     */
    public List<TaskResponseDTO> getByAssignedUser(Long projectId, Long assignedTo) {
        return toResponseList(repository.findByProjectIdAndAssignedTo(projectId, assignedTo));
    }

    /**
     * Obtiene una tarea puntual por su id
     *
     * @param taskId
     * @return tarea encontrada, null si no existe
     */
    public TaskResponseDTO getById(Long taskId) {
        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null) {
            return null;
        }
        return toResponse(entity);
    }

    /**
     * Calcula la siguiente posicion disponible dentro de una columna
     *
     * @param columnId
     * @return siguiente posicion
     */
    private Long nextPosition(Long columnId) {
        List<TaskEntity> tasks = repository.findByColumnIdOrderByPositionAsc(columnId);
        if (tasks.isEmpty()) {
            return 0L;
        }
        return tasks.get(tasks.size() - 1).getPosition() + 1;
    }

    /**
     * Crea una nueva tarea en una columna del proyecto.
     * Si no se envia posicion, se calcula automaticamente al final de la columna.
     *
     * @param projectId
     * @param createdBy id del usuario que crea la tarea
     * @param dto
     * @return tarea creada
     */
    @Transactional
    public TaskResponseDTO create(Long projectId, Long createdBy, TaskRequestDTO dto) {

        TaskEntity entity = new TaskEntity();
        entity.setProjectId(projectId);
        entity.setColumnId(dto.getColumnId());
        entity.setSprintId(dto.getSprintId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setColor(dto.getColor());
        entity.setDueDate(dto.getDueDate());
        entity.setAssignedTo(dto.getAssignedTo());
        entity.setCreatedBy(createdBy);

        if (dto.getPosition() != null) {
            entity.setPosition(dto.getPosition());
        } else {
            entity.setPosition(nextPosition(dto.getColumnId()));
        }

        repository.save(entity);

        // Gancho para futuro: registrar en task_history y notificar si hay asignado

        return toResponse(entity);
    }

    /**
     * Actualiza los datos de una tarea existente.
     * No cambia columna ni posicion, para eso se usa el metodo move.
     *
     * @param taskId
     * @param dto
     * @return tarea actualizada, null si no existe
     */
    @Transactional
    public TaskResponseDTO update(Long taskId, TaskRequestDTO dto) {

        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null) {
            return null;
        }

        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getColor() != null) entity.setColor(dto.getColor());
        if (dto.getDueDate() != null) entity.setDueDate(dto.getDueDate());
        if (dto.getSprintId() != null) entity.setSprintId(dto.getSprintId());
        if (dto.getAssignedTo() != null) entity.setAssignedTo(dto.getAssignedTo());

        repository.save(entity);

        // Gancho para futuro: registrar en task_history si cambio el asignado

        return toResponse(entity);
    }

    /**
     * Mueve una tarea a otra columna y/o posicion (drag & drop del tablero).
     * Reindexa la columna de origen para no dejar huecos en el orden.
     *
     * @param taskId
     * @param dto columna y posicion destino
     * @return tarea movida, null si no existe
     */
    @Transactional
    public TaskResponseDTO move(Long taskId, TaskMoveDTO dto) {

        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null) {
            return null;
        }

        Long columnaAnterior = entity.getColumnId();
        Long posicionAnterior = entity.getPosition();

        entity.setColumnId(dto.getColumnId());
        entity.setPosition(dto.getPosition());
        repository.save(entity);

        if (!columnaAnterior.equals(dto.getColumnId())) {
            List<TaskEntity> restantes = repository.findByColumnIdAndPositionGreaterThan(columnaAnterior, posicionAnterior);
            for (TaskEntity restante : restantes) {
                restante.setPosition(restante.getPosition() - 1);
                repository.save(restante);
            }
        }

        // Gancho para futuro: registrar en task_history y notificar al asignado

        return toResponse(entity);
    }

    /**
     * Elimina una tarea y reindexa las posiciones restantes de su columna
     *
     * @param taskId
     */
    @Transactional
    public void delete(Long taskId) {

        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null) {
            return;
        }

        Long columnId = entity.getColumnId();
        Long posicionEliminada = entity.getPosition();

        repository.delete(entity);

        List<TaskEntity> restantes = repository.findByColumnIdAndPositionGreaterThan(columnId, posicionEliminada);
        for (TaskEntity restante : restantes) {
            restante.setPosition(restante.getPosition() - 1);
            repository.save(restante);
        }
    }

    /**
     * Convierte una lista de entidades en su lista de dtos de respuesta
     *
     * @param tasks
     * @return lista de dtos
     */
    private List<TaskResponseDTO> toResponseList(List<TaskEntity> tasks) {
        List<TaskResponseDTO> response = new ArrayList<>();
        for (TaskEntity task : tasks) {
            response.add(toResponse(task));
        }
        return response;
    }

    /**
     * Convierte una entidad en su dto de respuesta
     *
     * @param entity
     * @return dto con la informacion de la tarea
     */
    private TaskResponseDTO toResponse(TaskEntity entity) {
        TaskResponseDTO dto = new TaskResponseDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setColumnId(entity.getColumnId());
        dto.setSprintId(entity.getSprintId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setColor(entity.getColor());
        dto.setDueDate(entity.getDueDate());
        dto.setAssignedTo(entity.getAssignedTo());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setPosition(entity.getPosition());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}