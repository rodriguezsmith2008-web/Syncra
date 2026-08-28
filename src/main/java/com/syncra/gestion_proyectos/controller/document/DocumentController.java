package com.syncra.gestion_proyectos.controller.document;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.syncra.gestion_proyectos.dto.document.DocumentCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentCommentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentMessage;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentStatusUpdateDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.dto.activity.ActivityLogResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;
import com.syncra.gestion_proyectos.service.document.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
        private final ActivityLogService activityLogService;

    /**
     * Lista únicamente los documentos normales.
     */
    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getByProject(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                documentService.getByProject(projectId)
        );
    }

    /**
     * Lista únicamente las actas.
     */
    @GetMapping("/meeting-minutes")
    public ResponseEntity<List<DocumentResponseDTO>> getMeetingMinutes(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                documentService.getMeetingMinutes(projectId)
        );
    }

    /**
     * Obtiene un documento.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDTO> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                documentService.getById(id)
        );
    }

    /**
     * Obtiene los subdocumentos de un documento.
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<DocumentResponseDTO>> getChildren(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                documentService.getChildren(id)
        );
    }

        /** Historial detallado del documento, más reciente primero. */
        @GetMapping("/{id}/history")
        public ResponseEntity<List<ActivityLogResponseDTO>> getHistory(
                        @PathVariable Long projectId,
                        @PathVariable Long id) {
                return ResponseEntity.ok(activityLogService.getByDocument(projectId, id));
        }

    /**
     * Crear documento.
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<DocumentResponseDTO> create(
            @PathVariable Long projectId,
            @RequestBody DocumentRequestDTO dto,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.create(projectId, dto, userId));
    }

    /**
     * Buscar documentos.
     */
    @GetMapping("/search")
    public ResponseEntity<List<DocumentResponseDTO>> searchByTitle(
            @PathVariable Long projectId,
            @RequestParam String title) {

        return ResponseEntity.ok(
                documentService.searchByTitle(projectId, title)
        );
    }

    /**
     * Buscar actas.
     */
    @GetMapping("/meeting-minutes/search")
    public ResponseEntity<List<DocumentResponseDTO>> searchMeetingMinutes(
            @PathVariable Long projectId,
            @RequestParam String title) {

        return ResponseEntity.ok(
                documentService.searchMeetingMinutes(projectId, title)
        );
    }

    /**
     * Actualizar documento.
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{id}")
    public ResponseEntity<DocumentMessage> update(
            @PathVariable Long id,
            @RequestBody DocumentUpdateDTO dto,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.ok(
                documentService.update(id, dto, userId)
        );
    }

    /**
     * Eliminar documento.
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{id}")
    public ResponseEntity<DocumentMessage> delete(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                documentService.delete(id)
        );
    }

    /**
     * Comentarios del documento.
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<DocumentCommentResponseDTO>> getComments(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                documentService.getComments(id)
        );
    }

    /**
     * Agregar comentario.
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<DocumentMessage> addComment(
            @PathVariable Long id,
            @RequestBody DocumentCommentRequestDTO dto,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.addComment(id, dto, userId));
    }

    /**
     * Aprobar documento.
     */
    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PutMapping("/{documentId}/status")
    public ResponseEntity<DocumentResponseDTO> updateStatus(
            @PathVariable Long documentId,
            @RequestBody DocumentStatusUpdateDTO dto,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        return ResponseEntity.ok(
                documentService.updateStatus(documentId, dto, userId)
        );
    }

    /**
     * Descargar PDF.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id) throws Exception {

        byte[] pdfBytes = documentService.generatePdf(id);

        if (pdfBytes == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "documento.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}