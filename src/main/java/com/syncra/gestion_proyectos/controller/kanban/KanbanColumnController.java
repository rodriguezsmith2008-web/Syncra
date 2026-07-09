package com.syncra.gestion_proyectos.controller.kanban;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumMessage;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumRequestDTO;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.kanban.KanbanColumnService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/kanban-columns")
@RequiredArgsConstructor
public class KanbanColumnController {

    private final KanbanColumnService service;

    /**
     * Obtiene todas las columnas de un proyecto
     *
     * @param projectId
     * @return lista de columnas
     */
    @GetMapping
    public ResponseEntity<List<KanbaColumResponseDTO>> getAllColumsForProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getAllColumsForProject(projectId));
    }

    /**
     * Obtiene una columna puntual de un proyecto
     *
     * @param projectId
     * @param columId
     * @return mensaje con la columna encontrada
     */
    @GetMapping("/{columId}")
    public ResponseEntity<KanbaColumMessage<KanbaColumResponseDTO>> getAll(@PathVariable Long projectId,
            @PathVariable Long columId) {
        return ResponseEntity.ok(service.getAll(projectId, columId));
    }

    /**
     * Crea una nueva columna en un proyecto
     *
     * @param projectId
     * @param dto
     * @return mensaje con la columna creada
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<KanbaColumMessage<KanbaColumResponseDTO>> create(@PathVariable Long projectId,
            @RequestBody KanbaColumRequestDTO dto) {
        return ResponseEntity.ok(service.create(projectId, dto));
    }

    /**
     * Actualiza nombre, posicion o color de una columna
     *
     * @param projectId
     * @param columnId
     * @param dto
     * @return mensaje con la columna actualizada
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{columnId}")
    public ResponseEntity<KanbaColumMessage<KanbaColumResponseDTO>> update(@PathVariable Long projectId,
            @PathVariable Long columnId, @RequestBody KanbaColumRequestDTO dto) {
        return ResponseEntity.ok(service.update(projectId, columnId, dto));
    }

    /**
     * Reordena varias columnas de un proyecto a la vez 
     *
     * @param projectId
     * @param columns
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@PathVariable Long projectId,
            @RequestBody List<KanbaColumResponseDTO> columns) {
        service.reorder(projectId, columns);
        return ResponseEntity.noContent().build();
    }

    /**
     * Elimina una columna de un proyecto
     *
     * @param projectId
     * @param columnId
     * @return mensaje del resultado de la operacion
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{columnId}")
    public ResponseEntity<KanbaColumMessage<Void>> delete(@PathVariable Long projectId,
            @PathVariable Long columnId) {
        return ResponseEntity.ok(service.delete(projectId, columnId));
    }
}