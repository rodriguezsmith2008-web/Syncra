package com.syncra.gestion_proyectos.service.document;

import com.syncra.gestion_proyectos.repository.user.UsersRepository;
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
import java.io.ByteArrayOutputStream;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final UsersRepository usersRepository;

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
    public DocumentResponseDTO create(Long projectId, DocumentRequestDTO dto, Long createdBy) {

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título del documento es obligatorio.");
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

        DocumentEntity saved = documentRepository.save(newDocument);

        return toResponse(saved);
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

        // Traer nombre y avatar del usuario que comentó
        usersRepository.findById(commentEntity.getUserId()).ifPresent(user -> {
            response.setUserFullName(user.getFirstName() + " " + user.getLastName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        });

        // Calcular tiempo de visualización
        response.setTimeDisplay(calcularTimeDisplay(commentEntity.getCreatedAt()));

        return response;
    }

    private String calcularTimeDisplay(LocalDateTime createdAt) {
        LocalDateTime ahora = LocalDateTime.now();
        long horas = java.time.Duration.between(createdAt, ahora).toHours();
        long minutos = java.time.Duration.between(createdAt, ahora).toMinutes();

        if (horas >= 24) {

            return createdAt.format(
                    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy",
                            java.util.Locale.of("es", "CO")));
        } else if (horas >= 1) {
            return "hace " + horas + (horas == 1 ? " hora" : " horas");
        } else if (minutos >= 1) {
            return "hace " + minutos + (minutos == 1 ? " minuto" : " minutos");
        } else {
            return "justo ahora";
        }
    }

    /**
     * Aprueba o rechaza un documento
     *
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
    /**
     * Genera el PDF de un documento a partir de su contenido HTML
     *
     * @param id
     * @return bytes del PDF, null si el documento no existe
     */
    public byte[] generatePdf(Long id) throws Exception {

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);

        if (documentFound.isEmpty()) {
            return null;
        }

        DocumentEntity document = documentFound.get();

        String contenido = document.getContent()
                .replace("&oacute;", "ó")
                .replace("&aacute;", "á")
                .replace("&eacute;", "é")
                .replace("&iacute;", "í")
                .replace("&uacute;", "ú")
                .replace("&ntilde;", "ñ")
                .replace("&Aacute;", "Á")
                .replace("&Eacute;", "É")
                .replace("&Iacute;", "Í")
                .replace("&Oacute;", "Ó")
                .replace("&Uacute;", "Ú")
                .replace("&Ntilde;", "Ñ")
                .replace("&nbsp;", " ");

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        body { font-family: 'Helvetica', sans-serif; font-size: 12px; color: #222; }
                        h1 { font-size: 20px; border-bottom: 1px solid #ccc; padding-bottom: 8px; }
                        table { border-collapse: collapse; width: 100%%; }
                        td, th { border: 1px solid #ccc; padding: 4px; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    %s
                </body>
                </html>
                """.formatted(document.getTitle(), contenido);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }
}