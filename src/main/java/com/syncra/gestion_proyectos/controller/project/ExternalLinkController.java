package com.syncra.gestion_proyectos.controller.project;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.project.ExternalLinkRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ExternalLinkResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.project.ExternalLinkService;

import jakarta.servlet.http.HttpServletRequest;
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

    // servicio encargado de la lógica de negocio de los enlaces externos
    private final ExternalLinkService linkService;

    /**
     * Obtiene todos los enlaces asociados a un proyecto
     *
     * @param projectId
     * @return lista de enlaces externos
     */
    @GetMapping
    public ResponseEntity<List<ExternalLinkResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(linkService.getByProject(projectId));
    }

    /**
     * Obtiene un enlace específico de un proyecto
     *
     * @param projectId
     * @param linkId
     * @return info del enlace solicitado
     */
    @GetMapping("/{linkId}")
    public ResponseEntity<ExternalLinkResponseDTO> getById(
            @PathVariable Long projectId,
            @PathVariable Long linkId) {
        return ResponseEntity.ok(linkService.getById(projectId, linkId));
    }

    /**
     * Crea un nuevo enlace asociado a un proyecto
     *
     * @param projectId
     * @param dto
     * @param request
     * @return enlace creado
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<ExternalLinkResponseDTO> create(
            @PathVariable Long projectId,
            @RequestBody ExternalLinkRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(linkService.create(projectId, dto, userId));
    }

    /**
     * Actualiza la información de un enlace existente
     *
     * @param projectId
     * @param linkId
     * @param dto
     * @param request
     * @return enlace actualizado
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{linkId}")
    public ResponseEntity<ExternalLinkResponseDTO> update(
            @PathVariable Long projectId,
            @PathVariable Long linkId,
            @RequestBody ExternalLinkRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(linkService.update(projectId, linkId, dto, userId));
    }

    /**
     * Elimina un enlace asociado a un proyecto
     *
     * @param projectId
     * @param linkId
     * @param request
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long linkId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        linkService.delete(projectId, linkId, userId);
        return ResponseEntity.noContent().build();
    }
}