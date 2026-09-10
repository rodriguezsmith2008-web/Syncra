package com.syncra.gestion_proyectos.controller.document;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.syncra.gestion_proyectos.dto.document.DocTemplateResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocTemplateCreateDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.document.DocTemplateService;

import lombok.RequiredArgsConstructor;

/**
 * Controller de solo lectura para el catálogo de plantillas de documentos.
 * Las plantillas son fijas (cargadas por DocTemplateSeeder), por lo que
 * no se expone creación, edición ni eliminación por API.
 */
@RestController
@RequestMapping("/doc-templates")
@RequiredArgsConstructor
public class DocTemplateController {

    private final DocTemplateService docTemplateService;

    /**
     * Obtiene el catálogo completo de plantillas disponibles
     *
     * @return lista de plantillas
     */
    @GetMapping
    public ResponseEntity<List<DocTemplateResponseDTO>> listAll() {
        return ResponseEntity.ok(docTemplateService.listAll());
    }

    @RequireRole(RoleUserEnum.APPRENTICE)
    @GetMapping("/published")
    public ResponseEntity<List<DocTemplateResponseDTO>> listPublished() {
        return ResponseEntity.ok(docTemplateService.listPublished());
    }

    /**
     * Obtiene una plantilla puntual junto con su contenido base
     *
     * @param id
     * @return plantilla encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocTemplateResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(docTemplateService.getById(id));
    }

    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @GetMapping("/{id}/draft")
    public ResponseEntity<DocTemplateResponseDTO> getDraftById(@PathVariable Long id) {
        return ResponseEntity.ok(docTemplateService.getDraftById(id));
    }

    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PostMapping
    public ResponseEntity<DocTemplateResponseDTO> create(@RequestBody DocTemplateCreateDTO request) {
        return ResponseEntity.status(201).body(docTemplateService.create(request));
    }

    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PutMapping("/{id}")
    public ResponseEntity<DocTemplateResponseDTO> update(
            @PathVariable Long id,
            @RequestBody DocTemplateCreateDTO request) {
        return ResponseEntity.ok(docTemplateService.update(id, request));
    }

    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PostMapping("/{id}/publish")
    public ResponseEntity<DocTemplateResponseDTO> publish(@PathVariable Long id) {
        return ResponseEntity.ok(docTemplateService.publish(id));
    }

    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        docTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}