package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;

    /**
     * Obtiene todas las tareas de un proyecto, ordenadas por columna y posicion.
     * Es la que se usa para pintar el tablero kanban completo.
     *
     * @param projectId
     * @return lista de tareas del proyecto
     */
    public List<TaskResponseDTO> getByProject(Long projectId) {

        List<TaskEntity> tasks = repository.findByProjectIdOrderByColumnIdAscPositionAsc(projectId);

        return toResponseList(tasks);
    }

    /**
     * Obtiene las tareas de una columna especifica, ordenadas por posicion.
     * Util cuando el frontend solo necesita refrescar una columna del tablero.
     *
     * @param columnId
     * @return lista de tareas de la columna
     */
    public List<TaskResponseDTO> getByColumn(Long columnId) {

        List<TaskEntity> tasks = repository.findByColumnIdOrderByPositionAsc(columnId);

        return toResponseList(tasks);
    }

    /**
     * Obtiene las tareas asociadas a un sprint especifico.
     *
     * @param sprintId
     * @return lista de tareas del sprint
     */
    public List<TaskResponseDTO> getBySprint(Long sprintId) {

        List<TaskEntity> tasks = repository.findBySprintId(sprintId);

        return toResponseList(tasks);
    }

    /**
     * Obtiene las tareas de un proyecto asignadas a un usuario especifico.
     * Util para una vista tipo "mis tareas".
     *
     * @param projectId
     * @param assignedTo
     * @return lista de tareas asignadas al usuario
     */
    public List<TaskResponseDTO> getByAssignedUser(Long projectId, Long assignedTo) {

        List<TaskEntity> tasks = repository.findByProjectIdAndAssignedTo(projectId, assignedTo);

        return toResponseList(tasks);
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