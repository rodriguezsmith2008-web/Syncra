package com.syncra.gestion_proyectos.controller.project;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.project.ExternalLinkRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ExternalLinkResponseDTO;
import com.syncra.gestion_proyectos.service.project.ExternalLinkService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/projects/{projectId}/links")
@RequiredArgsConstructor
public class ExternalLinkController {

    //Servicio encargado de la lógica de negocio de los enlaces externos 
    private final ExternalLinkService linkService;

    /**
     * Obtiene todos los enlaces asociados a un proyecto
     * @param projectId id del proyecto
     * @return lista de enlaces externos
     */
    @GetMapping
    public ResponseEntity<List<ExternalLinkResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(linkService.getByProject(projectId));
    }

    /**
     * Obtiene un enlace específico de un proyecto
     * @param projectId
     * @param linkId id del enlace
     * @return info del enlace solicitado
     */
    @GetMapping("/{linkId")
    public ResponseEntity<ExternalLinkResponseDTO> getById(@PathVariable Long projectId, @PathVariable Long linkId) {
        return ResponseEntity.ok(linkService.getById(projectId, linkId));
    }

    /**
     * Crea un nuevo enlace asocuado a un proyecto
     * @param projectId
     * @param dto información del enlace
     * @return enlace creado
     */
    @PostMapping
    public ResponseEntity<ExternalLinkResponseDTO> create(@PathVariable Long projectId,
            @RequestBody ExternalLinkRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(linkService.create(projectId, dto, 1L));
    }

    /**
     * Actualiza la información de un enlace existente
     * @param projectId
     * @param linkId
     * @param dto nuevos datos del enlace
     * @return enlace actualizado
     */
    @PutMapping("/{linkId}")
    public ResponseEntity<ExternalLinkResponseDTO> update(@PathVariable Long projectId, @PathVariable Long linkId,
            @RequestBody ExternalLinkRequestDTO dto) {
        return ResponseEntity.ok(linkService.update(projectId, linkId, dto));
    }

    /**
     * Elimina un enlace asociado a un proyecto
     * @param projectId
     * @param linkId
     * @return respuesta sin contenido
     */
    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long linkId) {
        linkService.delete(projectId, linkId);
        return ResponseEntity.noContent().build();
    }

}
