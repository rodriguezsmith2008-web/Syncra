package com.syncra.gestion_proyectos.controller.project;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.project.ProjectRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectResponseDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectUpdateDTO;
import com.syncra.gestion_proyectos.dto.project.InstructorStatsResponseDTO;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.project.ProjectService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    // servicio encargado de la lógica de negocio de los proyectos
    private final ProjectService projectService;

    /**
     * Obtiene todos los proyectos registrados
     *
     * @return lista de proyectos
     */
    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @GetMapping
    public ResponseEntity<List<ProjectResponseDTO>> getAll() {
        return ResponseEntity.ok(projectService.getAll());
    }

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @GetMapping("/stats")
    public ResponseEntity<InstructorStatsResponseDTO> getInstructorStats() {
        return ResponseEntity.ok(projectService.getInstructorStats());
    }

    /**
     * Obtiene un proyecto por su id
     *
     * @param id
     * @return info del proyecto
     */

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public ResponseEntity<List<ProjectResponseDTO>> getByStatus(
            @PathVariable ProjectStatusEnum status) {

        return ResponseEntity.ok(projectService.getByStatus(status));
    }

    /**
     * Crea un nuevo proyecto
     *
     * @param dto
     * @param request
     * @return proyecto creado
     */
    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PostMapping
    public ResponseEntity<ProjectResponseDTO> create(
            @RequestBody ProjectRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(dto, userId));
    }

    /**
     * Actualiza la info de un proyecto existente
     *
     * @param id
     * @param dto
     * @return proyecto actualizado
     */

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDTO> update(@PathVariable Long id, @RequestBody ProjectUpdateDTO dto) {
        return ResponseEntity.ok(projectService.update(id, dto));
    }

    /**
     * Actualiza el estado de un proyecto
     *
     * @param id
     * @param status
     * @return proyecto actualizado
     */

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @PutMapping("/{id}/status")
    public ResponseEntity<ProjectResponseDTO> updateStatus(@PathVariable Long id,
            @RequestBody ProjectStatusEnum status) {
        return ResponseEntity.ok(projectService.updateStatus(id, status));
    }

    /**
     * Elimina un proyecto
     *
     * @param id
     * @return respuesta sin contenido
     */

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene los proyectos del usuario autenticado
     *
     * @return lista de proyectos donde el usuario es miembro
     */
    @GetMapping("/mine")
    public ResponseEntity<List<ProjectResponseDTO>> getMine(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(projectService.getMine(userId));
    }
}