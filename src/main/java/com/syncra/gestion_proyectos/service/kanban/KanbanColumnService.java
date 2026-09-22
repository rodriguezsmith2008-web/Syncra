package com.syncra.gestion_proyectos.service.kanban;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumMessage;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumRequestDTO;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumResponseDTO;
import com.syncra.gestion_proyectos.entity.kanban.KanbanColumnEntity;
import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;
import com.syncra.gestion_proyectos.repository.kanban.KanbanColumnRepository;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KanbanColumnService {

    private static final String COMPLETED_COLUMN_NAME = "Completada";
    private static final String TODO_COLUMN_NAME = "Por hacer";
    private static final String IN_PROGRESS_COLUMN_NAME = "En progreso";
    private static final String REVIEW_COLUMN_NAME = "En revisión";

    private final KanbanColumnRepository repository;
    private final ActivityLogService activityLogService;

    /**
     * Obtiene todas las columnas de un proyecto ordenadas por posicion
     *
     * @param projectid
     * @return lista de columnas
     */
    @Transactional
    public List<KanbaColumResponseDTO> getAllColumsForProject(Long projectid) {

        ensureCompletedColumn(projectid, null);

        List<KanbanColumnEntity> columsEntity = repository.findByProjectIdOrderByPositionAsc(projectid);
        List<KanbaColumResponseDTO> response = new ArrayList<>();

        for (KanbanColumnEntity columsofproject : columsEntity) {
            response.add(toResponse(columsofproject));
        }

        return response;
    }

    /**
     * Obtiene una columna puntual, validando que pertenezca al proyecto indicado
     *
     * @param projectId
     * @param columId
     * @return mensaje con la columna encontrada, o mensaje de error si no existe
     */
    public KanbaColumMessage<KanbaColumResponseDTO> getAll(Long projectId, Long columId) {

        KanbaColumMessage<KanbaColumResponseDTO> message = new KanbaColumMessage<>();

        KanbanColumnEntity entity = repository.findById(columId).orElse(null);

        if (entity == null) {
            message.setMessage("esta columna no se encuentra actualmente en la plataforma");
            return message;
        }

        if (!entity.getProjectId().equals(projectId)) {
            message.setMessage("no se encuentra columna de este proyecto");
            return message;
        }

        message.setData(toResponse(entity));
        message.setMessage("columna existente");

        return message;
    }

    /**
     * Calcula la siguiente posicion disponible dentro de un proyecto
     *
     * @param projectId
     * @return siguiente posicion
     */
    private Long nextPosition(Long projectId) {

        List<KanbanColumnEntity> columnas = repository.findByProjectIdOrderByPositionAsc(projectId);

        if (columnas.isEmpty()) {
            return 0L;
        }

        return columnas.get(columnas.size() - 1).getPosition() + 1;
    }

    @Transactional
    public void ensureCompletedColumn(Long projectId, Long userId) {
        ensureDefaultColumn(projectId, userId, TODO_COLUMN_NAME, "#64748b", false);
        ensureDefaultColumn(projectId, userId, IN_PROGRESS_COLUMN_NAME, "#2563eb", false);
        ensureDefaultColumn(projectId, userId, REVIEW_COLUMN_NAME, "#f59e0b", false);
        KanbanColumnEntity legacyCompleted = repository.findByProjectIdAndNameIgnoreCase(projectId, "Completado");
        if (legacyCompleted != null && repository.findByProjectIdAndNameIgnoreCase(projectId, COMPLETED_COLUMN_NAME) == null) {
            legacyCompleted.setName(COMPLETED_COLUMN_NAME);
            legacyCompleted.setColor("#16a34a");
            legacyCompleted.setIsFinal(true);
            repository.save(legacyCompleted);
        }
        ensureDefaultColumn(projectId, userId, COMPLETED_COLUMN_NAME, "#16a34a", true);
        moveCompletedColumnToEnd(projectId);
    }

    private void ensureDefaultColumn(Long projectId, Long userId, String name, String color, boolean isFinal) {
        if (repository.findByProjectIdAndNameIgnoreCase(projectId, name) != null) {
            return;
        }

        KanbanColumnEntity entity = new KanbanColumnEntity();
        entity.setProjectId(projectId);
        entity.setName(name);
        entity.setColor(color);
        entity.setIsFinal(isFinal);
        entity.setPosition(nextPosition(projectId));
        repository.save(entity);

        if (userId != null) {
            activityLogService.log(projectId, ActivityEntityTypeEnum.KANBAN_COLUMN, entity.getId(),
                    ActivityActionEnum.CREATED, "creó la columna \"" + name + "\"", userId);
        }
    }

    /**
     * Crea una nueva columna en un proyecto.
     * Si no se envia posicion, se calcula automaticamente al final.
     *
     * @param projectId
     * @param dto
     * @param userId    id del usuario que crea la columna
     * @return mensaje con la columna creada
     */
    @Transactional
    public KanbaColumMessage<KanbaColumResponseDTO> create(Long projectId, KanbaColumRequestDTO dto, Long userId) {

        KanbaColumMessage<KanbaColumResponseDTO> message = new KanbaColumMessage<>();
        ensureCompletedColumn(projectId, userId);

        KanbanColumnEntity entity = new KanbanColumnEntity();
        entity.setProjectId(projectId);
        entity.setName(dto.getName());
        entity.setColor(dto.getColor());
        entity.setIsFinal(false);
        entity.setPosition(nextPosition(projectId));

        repository.save(entity);
        moveCompletedColumnToEnd(projectId);

        activityLogService.log(projectId, ActivityEntityTypeEnum.KANBAN_COLUMN, entity.getId(),
                ActivityActionEnum.CREATED, "creó la columna \"" + entity.getName() + "\"", userId);

        message.setData(toResponse(entity));
        message.setMessage("columna creada exitosamente");

        return message;
    }

    /**
     * Actualiza nombre, posicion o color de una columna existente
     *
     * @param projectId
     * @param columnId
     * @param dto
     * @param userId    id del usuario que actualiza la columna
     * @return mensaje con la columna actualizada
     */
    @Transactional
    public KanbaColumMessage<KanbaColumResponseDTO> update(Long projectId, Long columnId, KanbaColumRequestDTO dto,
            Long userId) {

        KanbaColumMessage<KanbaColumResponseDTO> response = new KanbaColumMessage<>();

        KanbanColumnEntity entity = repository.findById(columnId).orElse(null);

        if (entity == null || !entity.getProjectId().equals(projectId)) {
            response.setMessage("La columna no existe o no pertenece al proyecto");
            return response;
        }

        if (isCompleted(entity)) {
            throw protectedColumnException();
        }

        if (dto.getName() != null)
            entity.setName(dto.getName());
        if (dto.getPosition() != null)
            entity.setPosition(dto.getPosition());
        if (dto.getColor() != null)
            entity.setColor(dto.getColor());
        entity.setIsFinal(false);

        repository.save(entity);
        moveCompletedColumnToEnd(projectId);

        activityLogService.log(projectId, ActivityEntityTypeEnum.KANBAN_COLUMN, entity.getId(),
                ActivityActionEnum.UPDATED, "editó la columna \"" + entity.getName() + "\"", userId);

        response.setData(toResponse(entity));
        response.setMessage("Se actualizo la columna correctamente");

        return response;
    }

    /**
     * Reordena varias columnas de un proyecto a la vez (drag & drop)
     *
     * @param projectId
     * @param columns   lista de columnas con su nueva posicion
     */
    @Transactional
    public void reorder(Long projectId, List<KanbaColumResponseDTO> columns) {

        ensureCompletedColumn(projectId, null);

        for (KanbaColumResponseDTO item : columns) {

            KanbanColumnEntity entity = repository.findById(item.getId()).orElse(null);

            if (entity == null || !entity.getProjectId().equals(projectId)) {
                continue;
            }

            if (isCompleted(entity)) {
                throw protectedColumnException();
            }

            entity.setPosition(item.getPosition());
            repository.save(entity);
        }

        moveCompletedColumnToEnd(projectId);
    }

    /**
     * Elimina una columna de un proyecto y reindexa las posiciones restantes
     *
     * @param projectId
     * @param columnId
     * @param userId    id del usuario que elimina la columna
     * @return mensaje del resultado de la operacion
     */
    @Transactional
    public KanbaColumMessage<Void> delete(Long projectId, Long columnId, Long userId) {

        KanbaColumMessage<Void> message = new KanbaColumMessage<>();

        KanbanColumnEntity columna = repository.findById(columnId).orElse(null);

        if (columna == null || !columna.getProjectId().equals(projectId)) {
            message.setMessage("La columna no existe o no pertenece al proyecto");
            return message;
        }

        if (isCompleted(columna)) {
            throw protectedColumnException();
        }

        String nombre = columna.getName();
        Long posicionEliminada = columna.getPosition();
        repository.delete(columna);

        List<KanbanColumnEntity> restantes = repository.findByProjectIdOrderByPositionAsc(projectId);

        for (KanbanColumnEntity restante : restantes) {
            if (restante.getPosition() > posicionEliminada) {
                restante.setPosition(restante.getPosition() - 1);
                repository.save(restante);
            }
        }

        ensureCompletedColumn(projectId, userId);
        moveCompletedColumnToEnd(projectId);

        activityLogService.log(projectId, ActivityEntityTypeEnum.KANBAN_COLUMN, columnId,
                ActivityActionEnum.DELETED, "eliminó la columna \"" + nombre + "\"", userId);

        message.setMessage("Columna eliminada correctamente");
        return message;
    }

    private boolean isCompleted(KanbanColumnEntity entity) {
        return COMPLETED_COLUMN_NAME.equalsIgnoreCase(entity.getName()) || Boolean.TRUE.equals(entity.getIsFinal());
    }

    private ResponseStatusException protectedColumnException() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "La columna Completado no se puede editar, reordenar ni eliminar");
    }

    private void moveCompletedColumnToEnd(Long projectId) {
        KanbanColumnEntity completed = repository.findByProjectIdAndNameIgnoreCase(projectId, COMPLETED_COLUMN_NAME);
        if (completed == null) {
            return;
        }

        List<KanbanColumnEntity> columns = repository.findByProjectIdOrderByPositionAsc(projectId);
        long position = 0;
        for (KanbanColumnEntity column : columns) {
            if (!column.getId().equals(completed.getId())) {
                column.setPosition(position++);
                column.setIsFinal(false);
                repository.save(column);
            }
        }

        completed.setPosition(position);
        completed.setIsFinal(true);
        repository.save(completed);
    }

    /**
     * Convierte una entidad en su dto de respuesta
     *
     * @param entity
     * @return dto con la informacion de la columna
     */
    private KanbaColumResponseDTO toResponse(KanbanColumnEntity entity) {

        KanbaColumResponseDTO dto = new KanbaColumResponseDTO();

        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setPosition(entity.getPosition());
        dto.setColor(entity.getColor());
        dto.setProjectId(entity.getProjectId());
        dto.setIsFinal(entity.getIsFinal());

        return dto;
    }
}