package com.syncra.gestion_proyectos.controller.document;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.document.DocumentCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentCommentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentMessage;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentStatusUpdateDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.document.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    /** Servicio de documentos */
    private final DocumentService documentService;

    /**
     * Obtiene todos los documentos activos de un proyecto
     *
     * @param projectId
     * @return lista de documentos
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(documentService.getByProject(projectId));
    }

    /**
     * Obtiene un documento por su id
     *
     * @param id
     * @return documento encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    /**
     * Crea un nuevo documento en un proyecto 
     *
     * @param projectId
     * @param dto
     * @param request
     * @return mensaje de respuesta
     */
    @PostMapping
    public ResponseEntity<DocumentMessage> create(@PathVariable Long projectId, @RequestBody DocumentRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.create(projectId, dto, userId));
    }

    /**
     * Busca documentos de un proyecto por título
     *
     * @param projectId
     * @param title     texto a buscar
     * @return lista de documentos encontrados
     */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponseDTO>> searchByTitle(
            @PathVariable Long projectId,
            @RequestParam String title) {
        return ResponseEntity.ok(documentService.searchByTitle(projectId, title));
    }

    /**
     * Actualiza el título o contenido de un documento 
     *
     * @param id
     * @param dto
     * @param request
     * @return mensaje de respuesta
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentMessage> update(@PathVariable Long id, @RequestBody DocumentUpdateDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(documentService.update(id, dto, userId));
    }

    /**
     * Elimina un documento 
     *
     * @param id
     * @return mensaje de respuesta
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DocumentMessage> delete(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.delete(id));
    }

    /**
     * Obtiene todos los comentarios de un documento
     *
     * @param id
     * @return lista de comentarios
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<DocumentCommentResponseDTO>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getComments(id));
    }

    /**
     * Agrega un comentario a un documento
     *
     * @param id
     * @param dto
     * @param request
     * @return mensaje de respuesta
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<DocumentMessage> addComment(@PathVariable Long id, @RequestBody DocumentCommentRequestDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.addComment(id, dto, userId));
    }

    /**
 * Aprueba o rechaza un documento solo instructor
 *
 * @param documentId
 * @param dto
 * @return documento actualizado
 */
@RequireRole(RoleUserEnum.INSTRUCTOR)
@PutMapping("/{documentId}/status")
public ResponseEntity<DocumentResponseDTO> updateStatus(@PathVariable Long documentId,
        @RequestBody DocumentStatusUpdateDTO dto) {
    return ResponseEntity.ok(documentService.updateStatus(documentId, dto));
}
}