package com.syncra.gestion_proyectos.controller.document;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.document.DocTemplateResponseDTO;
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
}