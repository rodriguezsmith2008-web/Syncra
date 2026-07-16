package com.syncra.gestion_proyectos.service.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.task.TaskHistoryResponseDTO;
import com.syncra.gestion_proyectos.entity.task.TaskHistoryEntity;
import com.syncra.gestion_proyectos.repository.task.TaskHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskHistoryService {

    private final TaskHistoryRepository repository;

    /**
     * Obtiene el historial completo de una tarea
     *
     * @param taskId
     * @return lista de registros de historial
     */
    public List<TaskHistoryResponseDTO> getByTask(Long taskId) {

        List<TaskHistoryEntity> historial = repository.findByTaskIdOrderByCreatedAtDesc(taskId);
        List<TaskHistoryResponseDTO> response = new ArrayList<>();

        for (TaskHistoryEntity registro : historial) {
            response.add(toResponse(registro));
        }

        return response;
    }

    /**
     * Registra un nuevo evento en el historial de una tarea.
     * No se expone por controller: se llama internamente desde TaskService
     * cada vez que se crea, edita o mueve una tarea.
     *
     * @param taskId
     * @param userId id del usuario que hizo el cambio
     * @param action tipo de accion (ej: "CREATED", "MOVED", "ASSIGNED")
     * @param fromValue valor anterior (puede ser null)
     * @param toValue valor nuevo (puede ser null)
     */
    public void registrar(Long taskId, Long userId, String action, String fromValue, String toValue) {

        TaskHistoryEntity entity = new TaskHistoryEntity();
        entity.setTaskId(taskId);
        entity.setUserId(userId);
        entity.setAction(action);
        entity.setFromValue(fromValue);
        entity.setToValue(toValue);

        repository.save(entity);
    }

    /**
     * Convierte una entidad en su dto de respuesta
     *
     * @param entity
     * @return dto con la informacion del registro de historial
     */
    private TaskHistoryResponseDTO toResponse(TaskHistoryEntity entity) {

        TaskHistoryResponseDTO dto = new TaskHistoryResponseDTO();

        dto.setId(entity.getId());
        dto.setTaskId(entity.getTaskId());
        dto.setUserId(entity.getUserId());
        dto.setAction(entity.getAction());
        dto.setFromValue(entity.getFromValue());
        dto.setToValue(entity.getToValue());
        dto.setCreatedAt(entity.getCreatedAt());

        return dto;
    }
}