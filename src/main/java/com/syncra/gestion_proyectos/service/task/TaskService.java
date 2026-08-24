package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskMoveDTO;
import com.syncra.gestion_proyectos.dto.task.TaskRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;
    private final TaskHistoryService taskHistoryService;
    private final NotificationService notificationService;
        private final UsersRepository usersRepository;

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


        private void assignedUser(Long assignedTo) {

        if (assignedTo == null) {
            return;
        }

        UsersEntity usuario = usersRepository.findById(assignedTo)
                .orElseThrow(() -> new IllegalArgumentException("Usuario asignado no encontrado"));

        if (usuario.getRole() != RoleUserEnum.APPRENTICE) {
            throw new IllegalStateException("Solo se pueden asignar tareas a aprendices");
        }
    }

    /**
     * Crea una nueva tarea en una columna del proyecto.
     * Si no se envia posicion, se calcula automaticamente al final de la columna.
     * Registra el evento en el historial de la tarea.
     *
     * @param projectId
     * @param createdBy id del usuario que crea la tarea
     * @param dto
     * @return tarea creada
     */
    @Transactional
    public TaskResponseDTO create(Long projectId, Long createdBy, TaskRequestDTO dto) {

       assignedUser(dto.getAssignedTo());

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

        taskHistoryService.registrar(entity.getId(), createdBy, "CREATED", null, entity.getTitle());

        if (entity.getAssignedTo() != null && !entity.getAssignedTo().equals(createdBy)) {
            notificationService.crear(entity.getAssignedTo(), createdBy, entity.getProjectId(), entity.getId(), null, null,
                    "TASK_ASSIGNED", "Te asigno la tarea: " + entity.getTitle());
        }

        return toResponse(entity);
    }
    /**
     * Actualiza los datos de una tarea existente.
     * No cambia columna ni posicion, para eso se usa el metodo move.
     * Si cambia el usuario asignado, registra ese cambio en el historial.
     *
     * @param taskId
     * @param userId id del usuario que hace la edicion
     * @param dto
     * @return tarea actualizada, null si no existe
     */
   @Transactional
    public TaskResponseDTO update(Long taskId, Long userId, TaskRequestDTO dto) {

        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null) {
            return null;
        }

        Long asignadoAnterior = entity.getAssignedTo();

        if (dto.getTitle() != null)
            entity.setTitle(dto.getTitle());
        if (dto.getDescription() != null)
            entity.setDescription(dto.getDescription());
        if (dto.getColor() != null)
            entity.setColor(dto.getColor());

        entity.setDueDate(dto.getDueDate());

        if (dto.getSprintId() != null)
            entity.setSprintId(dto.getSprintId());

        if (dto.getAssignedTo() != null) {
            assignedUser(dto.getAssignedTo());
            entity.setAssignedTo(dto.getAssignedTo());
        }

        repository.save(entity);

        if (dto.getAssignedTo() != null && !dto.getAssignedTo().equals(asignadoAnterior)) {

            String valorAnterior = asignadoAnterior != null ? asignadoAnterior.toString() : null;
            String valorNuevo = dto.getAssignedTo().toString();

            taskHistoryService.registrar(taskId, userId, "ASSIGNED", valorAnterior, valorNuevo);

            if (!dto.getAssignedTo().equals(userId)) {
                notificationService.crear(dto.getAssignedTo(), userId, entity.getProjectId(), taskId, null, null,
                        "TASK_ASSIGNED", "Te asigno la tarea: " + entity.getTitle());
            }
        }

        return toResponse(entity);
    }

    /**
     * Mueve una tarea a otra columna y/o posicion (drag & drop del tablero).
     * Reindexa la columna de origen para no dejar huecos en el orden.
     * Registra el movimiento en el historial de la tarea.
     *
     * @param taskId
     * @param userId id del usuario que mueve la tarea
     * @param dto    columna y posicion destino
     * @return tarea movida, null si no existe
     */
@Transactional
public TaskResponseDTO move(Long taskId, Long userId, TaskMoveDTO dto) {

    TaskEntity entity = repository.findById(taskId).orElse(null);
    if (entity == null) {
        return null;
    }

    Long columnaAnterior = entity.getColumnId();
    Long posicionAnterior = entity.getPosition();
    Long columnaDestino = dto.getColumnId();
    Long posicionDestino = dto.getPosition();

    boolean cambioDeColumna = !columnaAnterior.equals(columnaDestino);

    if (cambioDeColumna) {

        List<TaskEntity> restantesOrigen = repository.findByColumnIdAndPositionGreaterThan(columnaAnterior,
                posicionAnterior);
        for (TaskEntity restante : restantesOrigen) {
            restante.setPosition(restante.getPosition() - 1);
            repository.save(restante);
        }

        List<TaskEntity> restantesDestino = repository.findByColumnIdAndPositionGreaterThanEqual(columnaDestino,
                posicionDestino);
        for (TaskEntity restante : restantesDestino) {
            if (!restante.getId().equals(taskId)) {
                restante.setPosition(restante.getPosition() + 1);
                repository.save(restante);
            }
        }

        taskHistoryService.registrar(taskId, userId, "MOVED", columnaAnterior.toString(),
                columnaDestino.toString());

       if (entity.getAssignedTo() != null && !entity.getAssignedTo().equals(userId)) {
    notificationService.crear(entity.getAssignedTo(), userId, entity.getProjectId(), entity.getId(), null, null,
            "TASK_MOVED", "La tarea '" + entity.getTitle() + "' cambio de columna");
}

    } else if (!posicionAnterior.equals(posicionDestino)) {

        if (posicionDestino > posicionAnterior) {
            List<TaskEntity> entreMedio = repository.findByColumnIdAndPositionBetween(columnaAnterior,
                    posicionAnterior + 1, posicionDestino);
            for (TaskEntity t : entreMedio) {
                t.setPosition(t.getPosition() - 1);
                repository.save(t);
            }
        } else {
            List<TaskEntity> entreMedio = repository.findByColumnIdAndPositionBetween(columnaAnterior,
                    posicionDestino, posicionAnterior - 1);
            for (TaskEntity t : entreMedio) {
                t.setPosition(t.getPosition() + 1);
                repository.save(t);
            }
        }
    }

    entity.setColumnId(columnaDestino);
    entity.setPosition(posicionDestino);
    repository.save(entity);

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

    public TaskResponseDTO unassign(Long taskId, Long userId) {
        TaskEntity entity = repository.findById(taskId).orElse(null);
        if (entity == null)
            return null;

        entity.setAssignedTo(null);
        repository.save(entity);

        return toResponse(entity);
    }
}