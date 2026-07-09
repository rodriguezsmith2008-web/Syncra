package com.syncra.gestion_proyectos.service.kanban;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumMessage;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumRequestDTO;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumResponseDTO;
import com.syncra.gestion_proyectos.entity.kanban.KanbanColumnEntity;
import com.syncra.gestion_proyectos.repository.kanban.KanbanColumnRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KanbanColumnService {

    private final KanbanColumnRepository repository;

    /**
     * Obtiene todas las columnas de un proyecto ordenadas por posicion
     *
     * @param projectid
     * @return lista de columnas
     */
    public List<KanbaColumResponseDTO> getAllColumsForProject(Long projectid) {

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

    /**
     * Crea una nueva columna en un proyecto.
     * Si no se envia posicion, se calcula automaticamente al final.
     *
     * @param projectId
     * @param dto
     * @return mensaje con la columna creada
     */
    @Transactional
    public KanbaColumMessage<KanbaColumResponseDTO> create(Long projectId, KanbaColumRequestDTO dto) {

        KanbaColumMessage<KanbaColumResponseDTO> message = new KanbaColumMessage<>();

        KanbanColumnEntity entity = new KanbanColumnEntity();
        entity.setProjectId(projectId);
        entity.setName(dto.getName());
        entity.setColor(dto.getColor());

        if (dto.getPosition() != null) {
            entity.setPosition(dto.getPosition());
        } else {
            entity.setPosition(nextPosition(projectId));
        }

        repository.save(entity);

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
     * @return mensaje con la columna actualizada
     */
    @Transactional
    public KanbaColumMessage<KanbaColumResponseDTO> update(Long projectId, Long columnId, KanbaColumRequestDTO dto) {

        KanbaColumMessage<KanbaColumResponseDTO> response = new KanbaColumMessage<>();

        KanbanColumnEntity entity = repository.findById(columnId).orElse(null);

        if (entity == null || !entity.getProjectId().equals(projectId)) {
            response.setMessage("La columna no existe o no pertenece al proyecto");
            return response;
        }

        if (dto.getName() != null)
            entity.setName(dto.getName());
        if (dto.getPosition() != null)
            entity.setPosition(dto.getPosition());
        if (dto.getColor() != null)
            entity.setColor(dto.getColor());

        repository.save(entity);

        response.setData(toResponse(entity));
        response.setMessage("Se actualizo la columna correctamente");

        return response;
    }

    /**
     * Reordena varias columnas de un proyecto a la vez (drag & drop)
     *
     * @param projectId
     * @param columns lista de columnas con su nueva posicion
     */
    @Transactional
    public void reorder(Long projectId, List<KanbaColumResponseDTO> columns) {

        for (KanbaColumResponseDTO item : columns) {

            KanbanColumnEntity entity = repository.findById(item.getId()).orElse(null);

            if (entity == null || !entity.getProjectId().equals(projectId)) {
                continue;
            }

            entity.setPosition(item.getPosition());
            repository.save(entity);
        }
    }

    /**
     * Elimina una columna de un proyecto y reindexa las posiciones restantes
     *
     * @param projectId
     * @param columnId
     * @return mensaje del resultado de la operacion
     */
    @Transactional
    public KanbaColumMessage<Void> delete(Long projectId, Long columnId) {

        KanbaColumMessage<Void> message = new KanbaColumMessage<>();

        KanbanColumnEntity columna = repository.findById(columnId).orElse(null);

        if (columna == null || !columna.getProjectId().equals(projectId)) {
            message.setMessage("La columna no existe o no pertenece al proyecto");
            return message;
        }

        Long posicionEliminada = columna.getPosition();
        repository.delete(columna);

        List<KanbanColumnEntity> restantes = repository.findByProjectIdOrderByPositionAsc(projectId);

        for (KanbanColumnEntity restante : restantes) {
            if (restante.getPosition() > posicionEliminada) {
                restante.setPosition(restante.getPosition() - 1);
                repository.save(restante);
            }
        }

        message.setMessage("Columna eliminada correctamente");
        return message;
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

        return dto;
    }
}