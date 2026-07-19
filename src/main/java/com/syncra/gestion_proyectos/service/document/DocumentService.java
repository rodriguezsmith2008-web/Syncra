package com.syncra.gestion_proyectos.service.document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.document.DocumentCommentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentCommentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentMessage;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentStatusUpdateDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.entity.document.DocumentCommentEntity;
import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentCommentRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    /** Repositorio de documentos */
    private final DocumentRepository documentRepository;

    /** Repositorio de comentarios de documentos */
    private final DocumentCommentRepository documentCommentRepository;

    /** Repositorio de plantillas de documentos */
    private final DocTemplateRepository docTemplateRepository;

    /**
     * Obtiene todos los documentos activos de un proyecto
     *
     * @param projectId
     * @return lista de documentos
     */
    public List<DocumentResponseDTO> getByProject(Long projectId) {

        List<DocumentEntity> documentList = documentRepository.findByProjectIdAndDeletedAtIsNull(projectId);
        List<DocumentResponseDTO> response = new ArrayList<>();

        for (DocumentEntity documentEntity : documentList) {
            response.add(toResponse(documentEntity));
        }

        return response;
    }

    /**
     * Obtiene un documento activo por su id
     *
     * @param id
     * @return documento encontrado, null si no existe o fue eliminado
     */
    public DocumentResponseDTO getById(Long id) {

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);

        if (documentFound.isEmpty()) {
            return null;
        }

        return toResponse(documentFound.get());
    }

    /**
     * Crea un nuevo documento en un proyecto
     *
     * @param projectId
     * @param dto
     * @param createdBy id del usuario que crea el documento
     * @return mensaje de respuesta
     */
    public DocumentMessage create(Long projectId, DocumentRequestDTO dto, Long createdBy) {

        DocumentMessage message = new DocumentMessage();

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            message.setMessage("El título del documento es obligatorio.");
            return message;
        }

        DocumentEntity newDocument = new DocumentEntity();

        newDocument.setProjectId(projectId);
        newDocument.setTitle(dto.getTitle());
        newDocument.setTemplateId(dto.getTemplateId());
        newDocument.setCreatedBy(createdBy);
        newDocument.setUpdatedBy(createdBy);

        if (dto.getTemplateId() != null) {

            Optional<DocTemplateEntity> templateFound = docTemplateRepository.findById(dto.getTemplateId());

            if (templateFound.isPresent()) {
                newDocument.setContent(templateFound.get().getDefaultContent());
            }
        }

        documentRepository.save(newDocument);

        message.setMessage("Documento creado correctamente.");
        return message;
    }

    /**
     * Actualiza el título o contenido de un documento
     *
     * @param id
     * @param dto
     * @param updatedBy id del usuario que actualiza
     * @return mensaje de respuesta
     */
    public DocumentMessage update(Long id, DocumentUpdateDTO dto, Long updatedBy) {

        DocumentMessage message = new DocumentMessage();

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);

        if (documentFound.isEmpty()) {
            message.setMessage("Documento no encontrado.");
            return message;
        }

        DocumentEntity documentToUpdate = documentFound.get();

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            documentToUpdate.setTitle(dto.getTitle());
        }

        if (dto.getContent() != null) {
            documentToUpdate.setContent(dto.getContent());
        }

        documentToUpdate.setUpdatedBy(updatedBy);

        documentRepository.save(documentToUpdate);

        message.setMessage("Documento actualizado correctamente.");
        return message;
    }

    /**
     * Elimina un documento marcándolo con la fecha de eliminación
     *
     * @param id
     * @return mensaje de respuesta
     */
    public DocumentMessage delete(Long id) {

        DocumentMessage message = new DocumentMessage();

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);

        if (documentFound.isEmpty()) {
            message.setMessage("Documento no encontrado.");
            return message;
        }

        DocumentEntity documentToDelete = documentFound.get();
        documentToDelete.setDeletedAt(LocalDateTime.now());

        documentRepository.save(documentToDelete);

        message.setMessage("Documento eliminado correctamente.");
        return message;
    }

    /**
     * Obtiene todos los comentarios de un documento
     *
     * @param documentId
     * @return lista de comentarios
     */
    public List<DocumentCommentResponseDTO> getComments(Long documentId) {

        List<DocumentCommentEntity> commentList = documentCommentRepository.findByDocumentId(documentId);
        List<DocumentCommentResponseDTO> response = new ArrayList<>();

        for (DocumentCommentEntity commentEntity : commentList) {
            response.add(toCommentResponse(commentEntity));
        }

        return response;
    }

    /**
     * Agrega un comentario a un documento
     *
     * @param documentId
     * @param dto
     * @param userId     id del usuario que comenta
     * @return mensaje de respuesta
     */
    public DocumentMessage addComment(Long documentId, DocumentCommentRequestDTO dto, Long userId) {

        DocumentMessage message = new DocumentMessage();

        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            message.setMessage("El comentario no puede estar vacío.");
            return message;
        }

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(documentId);

        if (documentFound.isEmpty()) {
            message.setMessage("Documento no encontrado.");
            return message;
        }

        DocumentCommentEntity newComment = new DocumentCommentEntity();

        newComment.setDocumentId(documentId);
        newComment.setUserId(userId);
        newComment.setContent(dto.getContent());

        documentCommentRepository.save(newComment);

        message.setMessage("Comentario agregado correctamente.");
        return message;
    }

    /**
     * Busca documentos activos de un proyecto por título
     *
     * @param projectId
     * @param title     texto a buscar en el título
     * @return lista de documentos encontrados
     */
    public List<DocumentResponseDTO> searchByTitle(Long projectId, String title) {

        List<DocumentEntity> documentList = documentRepository
                .findByProjectIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(projectId, title);
        List<DocumentResponseDTO> response = new ArrayList<>();

        for (DocumentEntity documentEntity : documentList) {
            response.add(toResponse(documentEntity));
        }

        return response;
    }

    /**
     * Convierte una entidad en un dto de respuesta
     *
     * @param documentEntity entidad a convertir
     * @return dto con la información del documento
     */
    private DocumentResponseDTO toResponse(DocumentEntity documentEntity) {

        DocumentResponseDTO response = new DocumentResponseDTO();

        response.setId(documentEntity.getId());
        response.setProjectId(documentEntity.getProjectId());
        response.setTemplateId(documentEntity.getTemplateId());
        response.setTitle(documentEntity.getTitle());
        response.setContent(documentEntity.getContent());
        response.setStatus(documentEntity.getStatus());
        response.setCreatedBy(documentEntity.getCreatedBy());
        response.setUpdatedBy(documentEntity.getUpdatedBy());
        response.setCreatedAt(documentEntity.getCreatedAt());
        response.setUpdatedAt(documentEntity.getUpdatedAt());

        return response;
    }

    /**
     * Convierte una entidad de comentario en un dto de respuesta
     *
     * @param commentEntity entidad a convertir
     * @return dto con la información del comentario
     */
    private DocumentCommentResponseDTO toCommentResponse(DocumentCommentEntity commentEntity) {

        DocumentCommentResponseDTO response = new DocumentCommentResponseDTO();

        response.setId(commentEntity.getId());
        response.setDocumentId(commentEntity.getDocumentId());
        response.setUserId(commentEntity.getUserId());
        response.setContent(commentEntity.getContent());
        response.setCreatedAt(commentEntity.getCreatedAt());

        return response;
    }

    /**
     * Aprueba o rechaza un documento

     *
     * @param documentId
     * @param dto        estado nuevo 
     * @return documento actualizado, null si no existe
     */
    @Transactional
    public DocumentResponseDTO updateStatus(Long documentId, DocumentStatusUpdateDTO dto) {

        DocumentEntity entity = documentRepository.findById(documentId).orElse(null);

        if (entity == null) {
            return null;
        }

        entity.setStatus(dto.getStatus());
        documentRepository.save(entity);

        return toResponse(entity);
    }
}