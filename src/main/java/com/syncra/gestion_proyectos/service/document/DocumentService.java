package com.syncra.gestion_proyectos.service.document;

import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.sprint.SprintEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentCommentRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.sprint.SprintRepository;

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

    private final NotificationService notificationService;
    private final ProjectMemberRepository projectMemberRepository;
    private final SprintRepository sprintRepository;

    /**
     * Obtiene todos los documentos activos de un proyecto
     *
     * @param projectId
     * @return lista de documentos
     */
    public List<DocumentResponseDTO> getByProject(Long projectId) {

        List<DocumentEntity> documentList = documentRepository
                .findByProjectIdAndDocumentTypeAndParentDocumentIdIsNullAndDeletedAtIsNull(
                        projectId,
                        DocumentTypeEnum.DOCUMENT);
        List<DocumentResponseDTO> response = new ArrayList<>();

        for (DocumentEntity documentEntity : documentList) {
    response.add(toResponse(documentEntity, null));
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

    Optional<DocumentEntity> documentFound =
            documentRepository.findByIdAndDeletedAtIsNull(id);

    if (documentFound.isEmpty()) {
        return null;
    }

    DocumentEntity entity = documentFound.get();

    Map<Long, SprintEntity> sprintsById = null;

    if (entity.getSprintId() != null) {
        sprintsById = sprintRepository.findById(entity.getSprintId())
                .map(sprint -> Map.of(entity.getSprintId(), sprint))
                .orElse(null);
    }

    return toResponse(entity, sprintsById);
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

        if (dto.getDocumentType() == null) {
            dto.setDocumentType(DocumentTypeEnum.DOCUMENT);
        }

        DocumentEntity newDocument = new DocumentEntity();

        newDocument.setProjectId(projectId);
        newDocument.setTitle(dto.getTitle());
        newDocument.setParentDocumentId(dto.getParentDocumentId());
        newDocument.setSortOrder(0);
        newDocument.setDocumentType(dto.getDocumentType());
        newDocument.setCreatedBy(createdBy);
        newDocument.setUpdatedBy(createdBy);
        newDocument.setSprintId(dto.getSprintId());
        newDocument.setQuarter(dto.getQuarter());

        Optional<DocTemplateEntity> templateFound = Optional.empty();

        if (dto.getDocumentType() == DocumentTypeEnum.MEETING_MINUTES) {

            templateFound = docTemplateRepository.findByCode("PT-AR-01");

        } else if (dto.getTemplateId() != null) {

            templateFound = docTemplateRepository.findById(dto.getTemplateId());

        }

        if (templateFound.isPresent()) {
            newDocument.setTemplateId(templateFound.get().getId());
            newDocument.setContent(templateFound.get().getDefaultContent());
        }

        DocumentEntity saved = documentRepository.save(newDocument);

        return toResponse(saved, null);

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

        if (dto.isSprintIdProvided()) {
            documentToUpdate.setSprintId(dto.getSprintId());
        }

        if (dto.isQuarterProvided()) {
            documentToUpdate.setQuarter(dto.getQuarter());
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

        // Recolectar los ids de usuario únicos involucrados en los comentarios
        List<Long> userIds = commentList.stream()
                .map(DocumentCommentEntity::getUserId)
                .distinct()
                .toList();

        // Una sola consulta para traer todos los usuarios necesarios
        Map<Long, UsersEntity> usersById = usersRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        List<DocumentCommentResponseDTO> response = new ArrayList<>();

        for (DocumentCommentEntity commentEntity : commentList) {
            response.add(toCommentResponse(commentEntity, usersById.get(commentEntity.getUserId())));
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

        DocumentEntity document = documentFound.get();

        DocumentCommentEntity newComment = new DocumentCommentEntity();
        newComment.setDocumentId(documentId);
        newComment.setUserId(userId);
        newComment.setContent(dto.getContent());
        newComment.setParentCommentId(dto.getParentCommentId());

        documentCommentRepository.save(newComment);

        if (dto.getParentCommentId() != null) {

            documentCommentRepository.findById(dto.getParentCommentId()).ifPresent(parent -> {
                if (!parent.getUserId().equals(userId)) {
                    notificationService.crear(parent.getUserId(), userId, document.getProjectId(), null, documentId,
                            newComment.getId(), "DOCUMENT_COMMENT_REPLY", "Te respondio en: " + document.getTitle());
                }
            });

        } else {

            List<ProjectMemberEntity> members = projectMemberRepository.findByIdProjectId(document.getProjectId());

            for (ProjectMemberEntity member : members) {
                Long memberId = member.getId().getUserId();
                if (!memberId.equals(userId)) {
                    notificationService.crear(memberId, userId, document.getProjectId(), null, documentId, null,
                            "DOCUMENT_COMMENT", "Nuevo comentario en: " + document.getTitle());
                }
            }

        }

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
                .findByProjectIdAndDocumentTypeAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                        projectId,
                        DocumentTypeEnum.DOCUMENT,
                        title);
        List<DocumentResponseDTO> response = new ArrayList<>();

       for (DocumentEntity documentEntity : documentList) {
    response.add(toResponse(documentEntity, null));
}


        return response;
    }

    /**
     * Convierte una entidad en un dto de respuesta
     *
     * @param documentEntity entidad a convertir
     * @return dto con la información del documento
     */
  private DocumentResponseDTO toResponse(
        DocumentEntity documentEntity,
        Map<Long, SprintEntity> sprintsById) {

    DocumentResponseDTO response = new DocumentResponseDTO();

    response.setId(documentEntity.getId());
    response.setProjectId(documentEntity.getProjectId());
    response.setTemplateId(documentEntity.getTemplateId());
    response.setTitle(documentEntity.getTitle());
    response.setContent(documentEntity.getContent());
    response.setStatus(documentEntity.getStatus());
    response.setDocumentType(documentEntity.getDocumentType());
    response.setCreatedBy(documentEntity.getCreatedBy());
    response.setUpdatedBy(documentEntity.getUpdatedBy());
    response.setParentDocumentId(documentEntity.getParentDocumentId());
    response.setSortOrder(documentEntity.getSortOrder());
    response.setCreatedAt(documentEntity.getCreatedAt());
    response.setUpdatedAt(documentEntity.getUpdatedAt());
    response.setSprintId(documentEntity.getSprintId());
    response.setQuarter(documentEntity.getQuarter());

    if (documentEntity.getSprintId() != null && sprintsById != null) {
        SprintEntity sprint = sprintsById.get(documentEntity.getSprintId());

        if (sprint != null) {
            response.setSprintName(sprint.getName());
        }
    }

    return response;
}


    /**
     * Convierte una entidad de comentario en un dto de respuesta
     *
     * @param commentEntity entidad a convertir
     * @return dto con la información del comentario
     */
    private DocumentCommentResponseDTO toCommentResponse(DocumentCommentEntity commentEntity, UsersEntity user) {

        DocumentCommentResponseDTO response = new DocumentCommentResponseDTO();

        response.setId(commentEntity.getId());
        response.setDocumentId(commentEntity.getDocumentId());
        response.setUserId(commentEntity.getUserId());
        response.setContent(commentEntity.getContent());
        response.setCreatedAt(commentEntity.getCreatedAt());
        response.setParentCommentId(commentEntity.getParentCommentId());

        if (user != null) {
            response.setUserFullName(user.getFirstName() + " " + user.getLastName());
            response.setUserAvatarUrl(user.getAvatarUrl());
        }

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
public DocumentResponseDTO updateStatus(
        Long documentId,
        DocumentStatusUpdateDTO dto) {

    DocumentEntity entity = documentRepository.findById(documentId).orElse(null);

    if (entity == null) {
        return null;
    }

    entity.setStatus(dto.getStatus());

    DocumentEntity saved = documentRepository.save(entity);

    return toResponse(saved, null);
}


    /**
     * Genera el PDF de un documento a partir de su contenido HTML
     *
     * @param id
     * @return bytes del PDF, null si el documento no existe
     */
    public byte[] generatePdf(Long id) throws Exception {

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);
        if (documentFound.isEmpty())
            return null;

        DocumentEntity document = documentFound.get();

        String contenido = document.getContent()
                .replaceAll("<img([^>]*[^/])>", "<img$1/>")
                .replaceAll("<img>", "<img/>")
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

        contenido = embedImagenesComoBase64(contenido);

        contenido = contenido.replaceAll("<col\\b([^>]*)>", "<col$1/>");

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                    <style>
                        body { font-family: 'Helvetica', sans-serif; font-size: 12px; color: #222; }
                        h1 { font-size: 20px; border-bottom: 1px solid #ccc; padding-bottom: 8px; }
                        img { max-width: 100%%; height: auto; }
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
        System.out.println(html);
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }

    private String embedImagenesComoBase64(String content) {
        if (content == null)
            return "";

        java.util.regex.Pattern pattern = java.util.regex.Pattern
                .compile("src=[\"']([^\"']+)[\"']");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String url = matcher.group(1);

            if (!url.startsWith("http")) {
                matcher.appendReplacement(result,
                        java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            try {
                // Si es URL de Cloudinary, forzar conversión a JPEG
                // Cloudinary permite transformaciones en la URL
                String urlDescarga = url;
                if (url.contains("cloudinary.com")) {

                    urlDescarga = url.replace("/upload/", "/upload/f_jpg,q_85/");
                }

                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URI(urlDescarga).toURL()
                        .openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.connect();

                byte[] bytes = conn.getInputStream().readAllBytes();

                // Siempre jpeg porque forzamos la conversión en Cloudinary
                String ext = "jpeg";
                String contentType = conn.getContentType();
                if (contentType != null && contentType.contains("png"))
                    ext = "png";

                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String dataUrl = "data:image/" + ext + ";base64," + base64;

                matcher.appendReplacement(result,
                        java.util.regex.Matcher.quoteReplacement("src=\"" + dataUrl + "\""));

            } catch (Exception e) {
                System.out.println("ERROR descargando imagen: " + url + " - " + e.getMessage());
                matcher.appendReplacement(result,
                        java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public List<DocumentResponseDTO> getChildren(Long documentId) {

        List<DocumentEntity> children = documentRepository
                .findByParentDocumentIdAndDocumentTypeAndDeletedAtIsNullOrderBySortOrderAsc(
                        documentId,
                        DocumentTypeEnum.DOCUMENT);

        List<DocumentResponseDTO> response = new ArrayList<>();

       for (DocumentEntity child : children) {
    response.add(toResponse(child, null));
}


        return response;
    }

   public List<DocumentResponseDTO> getMeetingMinutes(Long projectId) {

    List<DocumentEntity> minutes = documentRepository
            .findByProjectIdAndDocumentTypeAndDeletedAtIsNull(
                    projectId,
                    DocumentTypeEnum.MEETING_MINUTES);

    List<Long> sprintIds = minutes.stream()
            .map(DocumentEntity::getSprintId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();

    Map<Long, SprintEntity> sprintsById = sprintRepository.findAllById(sprintIds).stream()
            .collect(java.util.stream.Collectors.toMap(SprintEntity::getId, s -> s));

    List<DocumentResponseDTO> response = new ArrayList<>();

    for (DocumentEntity entity : minutes) {
        response.add(toResponse(entity, sprintsById));
    }

    return response;
}

    public List<DocumentResponseDTO> searchMeetingMinutes(Long projectId, String title) {

        List<DocumentEntity> minutes = documentRepository
                .findByProjectIdAndDocumentTypeAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                        projectId,
                        DocumentTypeEnum.MEETING_MINUTES,
                        title);

        List<DocumentResponseDTO> response = new ArrayList<>();

        for (DocumentEntity entity : minutes) {
    response.add(toResponse(entity, null));
}

        return response;
    }
}