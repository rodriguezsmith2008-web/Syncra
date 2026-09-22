package com.syncra.gestion_proyectos.service.document;

import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.service.notification.NotificationService;
import com.syncra.gestion_proyectos.service.realtime.DocumentRealtimeService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.document.DocumentCommentRequestDTO;
import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;
import com.syncra.gestion_proyectos.service.project.ProjectService;
import com.syncra.gestion_proyectos.dto.document.DocumentCommentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentMessage;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentStatusUpdateDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.entity.document.DocumentCommentEntity;
import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.dto.document.SectionMatchResponseDTO;
import com.syncra.gestion_proyectos.util.SectionContentUtil;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final UsersRepository usersRepository;

    /** Repositorio de documentos */
    private final DocumentRepository documentRepository;
    private final DocumentRealtimeService documentRealtimeService;

    /** Repositorio de comentarios de documentos */
    private final DocumentCommentRepository documentCommentRepository;

    /** Repositorio de plantillas de documentos */
    private final DocTemplateRepository docTemplateRepository;

    private final NotificationService notificationService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ActivityLogService activityLogService;
    private final SprintRepository sprintRepository;
    private final ProjectService projectService;

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

        Optional<DocumentEntity> documentFound = documentRepository.findByIdAndDeletedAtIsNull(id);

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

        if (dto.getParentDocumentId() != null) {
            DocumentEntity parent = documentRepository
                    .findByIdAndDeletedAtIsNull(dto.getParentDocumentId())
                    .orElseThrow(() -> new IllegalArgumentException("La página padre no existe."));

            if (!projectId.equals(parent.getProjectId())) {
                throw new IllegalArgumentException("La página padre pertenece a otro proyecto.");
            }

            if (parent.getDocumentType() != DocumentTypeEnum.DOCUMENT
                    || dto.getDocumentType() != DocumentTypeEnum.DOCUMENT) {
                throw new IllegalArgumentException(
                        "Dentro de una página solo se pueden crear documentos normales.");
            }
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
        newDocument.setMeetingType(dto.getMeetingType());

        Optional<DocTemplateEntity> templateFound = Optional.empty();

        if (dto.getTemplateId() != null) {
            templateFound = docTemplateRepository.findById(dto.getTemplateId());
        } else if (dto.getDocumentType() == DocumentTypeEnum.MEETING_MINUTES) {
            Optional<DocTemplateEntity> parentOpt = docTemplateRepository.findByCode("PT-AR-01");
            if (parentOpt.isPresent()) {
                Long parentId = parentOpt.get().getId();
                List<DocTemplateEntity> children = docTemplateRepository.findByParentTemplateIdOrderByPositionAsc(parentId);
                if (children != null && !children.isEmpty()) {
                    templateFound = Optional.of(children.get(children.size() - 1));
                } else {
                    List<DocTemplateEntity> legacyChildren = docTemplateRepository.findAll().stream()
                            .filter(t -> t.getDescription() != null && t.getDescription().trim().toLowerCase().startsWith("plantilla hija de"))
                            .toList();
                    if (!legacyChildren.isEmpty()) {
                        templateFound = Optional.of(legacyChildren.get(legacyChildren.size() - 1));
                    } else {
                        templateFound = parentOpt;
                    }
                }
            }
        }

        if (templateFound.isPresent()) {
            newDocument.setTemplateId(templateFound.get().getId());
            newDocument.setContent(templateFound.get().getDefaultContent());
        }

        DocumentEntity saved = documentRepository.save(newDocument);

        activityLogService.log(saved.getProjectId(), ActivityEntityTypeEnum.DOCUMENT, saved.getId(),
                ActivityActionEnum.CREATED, "creó el documento \"" + saved.getTitle() + "\"", createdBy);

        if (saved.getParentDocumentId() != null) {
            documentRepository.findByIdAndDeletedAtIsNull(saved.getParentDocumentId())
                    .ifPresent(parent -> activityLogService.log(parent.getProjectId(), ActivityEntityTypeEnum.DOCUMENT,
                            parent.getId(),
                            ActivityActionEnum.UPDATED,
                            "añadió el subdocumento \"" + saved.getTitle() + "\"",
                            createdBy));
        }

        materializarHijosDePlantilla(saved, createdBy);

        return toResponse(saved, null);

    }

    private void materializarHijosDePlantilla(DocumentEntity parent, Long createdBy) {
        String content = parent.getContent();
        if (content == null || content.isBlank()) {
            return;
        }

        Pattern markerPattern = Pattern.compile(
                "<div(?=[^>]*data-document-card)(?=[^>]*data-template-id=\"(\\d+)\")(?=[^>]*data-titulo=\"([^\"]*)\")[^>]*>");
        Matcher matcher = markerPattern.matcher(content);
        StringBuffer updatedContent = new StringBuffer();
        boolean changed = false;

        while (matcher.find()) {
            Long templateId = Long.valueOf(matcher.group(1));
            String childTitle = matcher.group(2);

            if (templateAlreadyExistsInParentChain(parent, templateId)) {
                matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(matcher.group()));
                continue;
            }

            DocumentRequestDTO childRequest = new DocumentRequestDTO();
            childRequest.setTitle(childTitle == null || childTitle.isBlank() ? "Documento" : childTitle);
            childRequest.setParentDocumentId(parent.getId());
            childRequest.setTemplateId(templateId);
            childRequest.setDocumentType(DocumentTypeEnum.DOCUMENT);

            DocumentResponseDTO child = create(parent.getProjectId(), childRequest, createdBy);
            String replacement = matcher.group()
                    .replaceFirst(">", " data-doc-id=\"" + child.getId() + "\">");
            matcher.appendReplacement(updatedContent, Matcher.quoteReplacement(replacement));
            changed = true;
        }

        if (changed) {
            matcher.appendTail(updatedContent);
            parent.setContent(updatedContent.toString());
            documentRepository.save(parent);
        }
    }

    private boolean templateAlreadyExistsInParentChain(DocumentEntity document, Long templateId) {
        Long parentId = document.getParentDocumentId();

        while (parentId != null) {
            Optional<DocumentEntity> parent = documentRepository.findByIdAndDeletedAtIsNull(parentId);
            if (parent.isEmpty()) {
                return false;
            }

            if (templateId.equals(parent.get().getTemplateId())) {
                return true;
            }

            parentId = parent.get().getParentDocumentId();
        }

        return false;
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
        String previousTitle = documentToUpdate.getTitle();
        String previousContent = documentToUpdate.getContent();
        StringBuilder changes = new StringBuilder();

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()
                && !dto.getTitle().trim().equals(previousTitle)) {
            documentToUpdate.setTitle(dto.getTitle());
            changes.append("cambió el título de \"").append(previousTitle).append("\" a \"")
                    .append(dto.getTitle().trim()).append("\"");
        }

        if (dto.getContent() != null && !dto.getContent().equals(previousContent)) {
            documentToUpdate.setContent(dto.getContent());
            appendChange(changes, describirCambioContenido(previousContent, dto.getContent()));
        }

        if (dto.isCoverImageUrlProvided()
                && !java.util.Objects.equals(dto.getCoverImageUrl(), documentToUpdate.getCoverImageUrl())) {
            documentToUpdate.setCoverImageUrl(dto.getCoverImageUrl());
            appendChange(changes, dto.getCoverImageUrl() == null
                    ? "eliminó la imagen de portada"
                    : "actualizó la imagen de portada");
        }

        if (dto.isSprintIdProvided()) {
            documentToUpdate.setSprintId(dto.getSprintId());
        }

        if (dto.isQuarterProvided()) {
            documentToUpdate.setQuarter(dto.getQuarter());
        }

        if (dto.isMeetingTypeProvided()) {
            documentToUpdate.setMeetingType(dto.getMeetingType());
        }

        documentToUpdate.setUpdatedBy(updatedBy);

        documentRepository.save(documentToUpdate);

        documentRealtimeService.publishUpdated(toResponse(documentToUpdate, null));

        String description = changes.length() == 0 ? "actualizó los datos del documento" : changes.toString();
        activityLogService.log(documentToUpdate.getProjectId(), ActivityEntityTypeEnum.DOCUMENT,
                documentToUpdate.getId(), ActivityActionEnum.UPDATED,
                description, updatedBy);

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

        activityLogService.log(documentToDelete.getProjectId(), ActivityEntityTypeEnum.DOCUMENT,
                documentToDelete.getId(), ActivityActionEnum.DELETED,
                "eliminó el documento \"" + documentToDelete.getTitle() + "\"", documentToDelete.getUpdatedBy());

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
        newComment.setAnchorId(dto.getAnchorId());
        newComment.setAnchorText(dto.getAnchorText());

        documentCommentRepository.save(newComment);
        message.setComment(toCommentResponse(
            newComment,
            usersRepository.findById(userId).orElse(null)));

        activityLogService.log(document.getProjectId(), ActivityEntityTypeEnum.DOCUMENT, documentId,
                ActivityActionEnum.UPDATED,
                (dto.getParentCommentId() == null ? "añadió un comentario" : "respondió un comentario")
                        + " en el documento \"" + document.getTitle() + "\"",
                userId);

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
        response.setCoverImageUrl(documentEntity.getCoverImageUrl());
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
        response.setMeetingType(documentEntity.getMeetingType());

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
        response.setAnchorId(commentEntity.getAnchorId());
        response.setAnchorText(commentEntity.getAnchorText());

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
            DocumentStatusUpdateDTO dto,
            Long updatedBy) {

        if (dto == null || dto.getStatus() == null
            || (dto.getStatus() != com.syncra.gestion_proyectos.enums.DocumentStatusEnum.DRAFT
                && dto.getStatus() != com.syncra.gestion_proyectos.enums.DocumentStatusEnum.APPROVED)) {
            throw new IllegalArgumentException("El estado del documento debe ser DRAFT o APPROVED.");
        }

        DocumentEntity entity = documentRepository.findById(documentId).orElse(null);

        if (entity == null) {
            return null;
        }

        entity.setStatus(dto.getStatus());

        DocumentEntity saved = documentRepository.save(entity);

        activityLogService.log(saved.getProjectId(), ActivityEntityTypeEnum.DOCUMENT, saved.getId(),
                ActivityActionEnum.UPDATED,
                "cambió el estado del documento a " + dto.getStatus().name(),
                updatedBy);

        DocumentResponseDTO response = toResponse(saved, null);
        documentRealtimeService.publishUpdated(response);

        if (dto.getStatus() == com.syncra.gestion_proyectos.enums.DocumentStatusEnum.APPROVED) {
            projectService.moveToReviewIfAllDocumentsApproved(saved.getProjectId());
        }

        return response;
    }

    private void appendChange(StringBuilder changes, String change) {
        if (changes.length() > 0) {
            changes.append("; ");
        }
        changes.append(change);
    }

    private String describirCambioContenido(String previousContent, String currentContent) {
        String previous = previousContent == null ? "" : previousContent;
        String current = currentContent == null ? "" : currentContent;
        int previousImages = countOccurrences(previous, "<img");
        int currentImages = countOccurrences(current, "<img");
        int previousTables = countOccurrences(previous, "<table");
        int currentTables = countOccurrences(current, "<table");
        int previousDocuments = countOccurrences(previous, "data-document-card");
        int currentDocuments = countOccurrences(current, "data-document-card");
        int previousFiles = countOccurrences(previous, "data-file-card");
        int currentFiles = countOccurrences(current, "data-file-card");
        List<String> changes = new ArrayList<>();

        if (currentImages > previousImages)
            changes.add("añadió " + (currentImages - previousImages) + " imagen(es)");
        if (currentImages < previousImages)
            changes.add("eliminó " + (previousImages - currentImages) + " imagen(es)");
        if (currentTables > previousTables)
            changes.add("añadió " + (currentTables - previousTables) + " tabla(s)");
        if (currentTables < previousTables)
            changes.add("eliminó " + (previousTables - currentTables) + " tabla(s)");
        if (currentDocuments > previousDocuments)
            changes.add("añadió " + (currentDocuments - previousDocuments) + " subdocumento(s) enlazado(s)");
        if (currentDocuments < previousDocuments)
            changes.add("eliminó " + (previousDocuments - currentDocuments) + " subdocumento(s) enlazado(s)");
        if (currentFiles > previousFiles)
            changes.add("añadió " + (currentFiles - previousFiles) + " archivo(s) adjunto(s)");
        if (currentFiles < previousFiles)
            changes.add("eliminó " + (previousFiles - currentFiles) + " archivo(s) adjunto(s)");
        agregarNombres(changes, previous, current, "data-titulo=", "página(s)");
        agregarNombres(changes, previous, current, "data-file-name=", "archivo(s)");

        registrarDiferencia(changes, previous, current, "<a ", "enlace(s)", "enlace(s)");
        registrarDiferencia(changes, previous, current, "<h", "encabezado(s)", "encabezado(s)");
        registrarDiferencia(changes, previous, current, "<ul", "lista(s) con viñetas", "lista(s) con viñetas");
        registrarDiferencia(changes, previous, current, "<ol", "lista(s) numerada(s)", "lista(s) numerada(s)");
        registrarDiferencia(changes, previous, current, "<blockquote", "cita(s)", "cita(s)");
        registrarDiferencia(changes, previous, current, "<pre", "bloque(s) de código", "bloque(s) de código");
        registrarDiferencia(changes, previous, current, "<hr", "separador(es)", "separador(es)");
        registrarDiferencia(changes, previous, current, "<strong", "texto(s) en negrita", "texto(s) en negrita");
        registrarDiferencia(changes, previous, current, "<em", "texto(s) en cursiva", "texto(s) en cursiva");
        registrarDiferencia(changes, previous, current, "<u", "texto(s) subrayado(s)", "texto(s) subrayado(s)");
        registrarDiferencia(changes, previous, current, "<s", "texto(s) tachado(s)", "texto(s) tachado(s)");
        registrarDiferencia(changes, previous, current, "data-text-align=", "alineación(es) de texto",
                "alineación(es) de texto");
        registrarDiferencia(changes, previous, current, "background-color", "resaltado(s) de texto",
                "resaltado(s) de texto");
        registrarDiferencia(changes, previous, current, "color:", "color(es) de texto", "color(es) de texto");

        String previousText = extraerTexto(previous);
        String currentText = extraerTexto(current);
        if (!previousText.equals(currentText))
            changes.add("modificó el texto y formato del contenido");
        return changes.isEmpty() ? "modificó el contenido del documento" : String.join(", ", changes);
    }

    private String extraerTexto(String html) {
        return html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void registrarDiferencia(List<String> changes, String previous, String current,
            String token, String addedLabel, String removedLabel) {
        int before = countOccurrences(previous, token);
        int after = countOccurrences(current, token);
        if (after > before)
            changes.add("añadió " + (after - before) + " " + addedLabel);
        if (after < before)
            changes.add("eliminó " + (before - after) + " " + removedLabel);
    }

    private void agregarNombres(List<String> changes, String previous, String current,
            String attribute, String label) {
        List<String> before = extraerValores(previous, attribute);
        List<String> after = extraerValores(current, attribute);
        List<String> added = new ArrayList<>(after);
        added.removeAll(before);
        List<String> removed = new ArrayList<>(before);
        removed.removeAll(after);
        if (!added.isEmpty())
            changes.add("añadió " + label + " " + String.join(", ", added));
        if (!removed.isEmpty())
            changes.add("eliminó " + label + " " + String.join(", ", removed));
    }

    private List<String> extraerValores(String html, String attribute) {
        List<String> values = new ArrayList<>();
        String search = attribute + "\"";
        int index = 0;
        while ((index = html.indexOf(search, index)) >= 0) {
            int start = index + search.length();
            int end = html.indexOf('"', start);
            if (end < 0)
                break;
            String value = html.substring(start, end).trim();
            if (!value.isEmpty() && !values.contains(value))
                values.add(value);
            index = end + 1;
        }
        return values;
    }

    private int countOccurrences(String value, String token) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
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

        if (document.getContent() == null || document.getContent().isBlank()) {
            String emptyHtml = "<p>No hay contenido para exportar.</p>";
            return generatePdfFromHtml(document.getTitle(), emptyHtml);
        }

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
        contenido = normalizarContenidoParaPdf(contenido);

        return generatePdfFromHtml(document.getTitle(), contenido);
    }

    private byte[] generatePdfFromHtml(String title, String contentHtml) {
        try {
            return renderPdf(title, contentHtml);
        } catch (Exception exception) {
            System.err.println("No se pudo renderizar el contenido enriquecido del PDF: " + exception.getMessage());
            try {
                return renderPdf(title, textoPlanoParaPdf(contentHtml));
            } catch (Exception fallbackException) {
                throw new IllegalStateException("No se pudo generar el PDF del documento", fallbackException);
            }
        }
    }

    private byte[] renderPdf(String title, String contentHtml) throws Exception {
        String safeTitle = escapeHtml(removerEmojis(title == null ? "Documento" : title));
        String safeContent = contentHtml == null ? "" : contentHtml;

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8" />
                                        <style>
                        @page {
                            size: A4;
                            margin: 2.2cm 2cm 2.5cm 2cm;
                            @bottom-center {
                                content: "Página " counter(page) " de " counter(pages);
                                font-family: 'Helvetica', sans-serif;
                                font-size: 9px;
                                color: #9ca3af;
                            }
                        }
                        body { font-family: 'Helvetica', sans-serif; font-size: 12px; color: #222; line-height: 1.5; }
                        h1 { font-size: 20px; border-bottom: 1px solid #ccc; padding-bottom: 8px; margin-bottom: 16px; }
                        h1, h2, h3, h4 { page-break-after: avoid; }
                        img { max-width: 100%%; height: auto; }
                        table { border-collapse: collapse; width: 100%%; margin: 8px 0; -fs-table-paginate: paginate; }
                        thead { display: table-header-group; }
                        tr { page-break-inside: avoid; }
                        td, th { border: 1px solid #ccc; padding: 6px; word-wrap: break-word; }
                        blockquote { border-left: 3px solid #94a3b8; margin: 10px 0; padding: 4px 12px; color: #475569; page-break-inside: avoid; }
                        pre { white-space: pre-wrap; font-family: monospace; background: #f3f4f6; padding: 8px; page-break-inside: avoid; }
                        a { color: #1d4ed8; text-decoration: underline; }
                        ul, ol { margin-top: 6px; margin-bottom: 6px; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    %s
                </body>
                </html>
                """
                .formatted(safeTitle, safeContent);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }

    private String textoPlanoParaPdf(String contentHtml) {
        String text = contentHtml == null ? "" : Jsoup.parse(contentHtml).text().trim();
        text = removerEmojis(text);
        if (text.isBlank()) {
            text = "No hay contenido para exportar.";
        }
        return "<p>" + escapeHtml(text).replace("\n", "<br/>") + "</p>";
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Rango de puntos de código Unicode que cubre emojis, banderas, pictogramas
     * y modificadores relacionados (tono de piel, selector de variación, ZWJ).
     * Helvetica (fuente usada por openhtmltopdf) no incluye estos glifos, por lo
     * que sin este filtro se renderizan como "#" (glifo .notdef).
     */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "[\\x{1F1E6}-\\x{1F1FF}" // banderas (indicadores regionales)
                    + "\\x{1F300}-\\x{1FAFF}" // símbolos misc, transporte, pictogramas suplementarios
                    + "\\x{2600}-\\x{27BF}" // símbolos misc y dingbats
                    + "\\x{2B00}-\\x{2BFF}" // flechas y símbolos misc adicionales
                    + "\\x{2300}-\\x{23FF}" // símbolos técnicos misc (reloj de arena, etc.)
                    + "\\x{FE0F}" // selector de variación (emoji vs texto)
                    + "\\x{200D}" // zero-width joiner (emojis compuestos)
                    + "\\x{1F900}-\\x{1F9FF}" // símbolos suplementarios y pictogramas
                    + "\\x{3030}\\x{303D}\\x{3297}\\x{3299}]");

    private String removerEmojis(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return EMOJI_PATTERN.matcher(text).replaceAll("");
    }

    private String normalizarContenidoParaPdf(String content) {
        String sinEmojis = removerEmojis(content);
        Document source = Jsoup.parseBodyFragment(sinEmojis == null ? "" : sinEmojis);

        for (Element fileCard : source.select("div[data-file-card]")) {
            String url = fileCard.attr("data-file-url");
            String name = fileCard.attr("data-file-name");
            Element paragraph = new Element(Tag.valueOf("p"), "");
            Element link = paragraph.appendElement("a").text("Archivo: " + (name.isBlank() ? "Adjunto" : name));
            if (!url.isBlank()) {
                link.attr("href", url);
            }
            fileCard.replaceWith(paragraph);
        }

        Safelist safelist = Safelist.none()
                .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "h1", "h2", "h3", "h4",
                        "blockquote", "ul", "ol", "li", "pre", "code", "table", "thead", "tbody", "tfoot",
                        "tr", "th", "td", "a", "img", "hr", "span")
                .addAttributes("a", "href", "title")
                .addAttributes("img", "src", "alt", "width", "height")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan")
                .addAttributes(":all", "style")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data");

        Document parsed = Jsoup.parseBodyFragment(Jsoup.clean(source.body().html(), "", safelist));
        for (Element element : parsed.body().getAllElements()) {
            element.removeAttr("class");
            element.removeAttr("id");
            String styleValue = element.attr("style");
            if (!styleValue.isBlank()) {
                String safeStyle = sanitizeStyle(styleValue);
                if (safeStyle.isBlank()) {
                    element.removeAttr("style");
                } else {
                    element.attr("style", safeStyle);
                }
            }
            if (element.is("img")) {
                String src = element.attr("src").toLowerCase(java.util.Locale.ROOT);
                if (src.startsWith("data:image/svg") || src.endsWith(".svg")) {
                    element.remove();
                }
            }
        }
        parsed.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(java.nio.charset.StandardCharsets.UTF_8)
                .prettyPrint(false);
        return parsed.body().html();
    }

    private static final java.util.Set<String> ALLOWED_PDF_STYLE_PROPERTIES = java.util.Set.of(
            "color", "background-color", "background", "font-weight", "font-style",
            "text-decoration", "text-align", "border-color");

    private String sanitizeStyle(String style) {
        StringBuilder result = new StringBuilder();
        for (String declaration : style.split(";")) {
            String[] parts = declaration.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String property = parts[0].trim().toLowerCase(java.util.Locale.ROOT);
            String value = parts[1].trim();
            String lowerValue = value.toLowerCase(java.util.Locale.ROOT);
            if (ALLOWED_PDF_STYLE_PROPERTIES.contains(property)
                    && !lowerValue.contains("url(")
                    && !lowerValue.contains("expression")) {
                result.append(property).append(": ").append(value).append("; ");
            }
        }
        return result.toString().trim();
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

    public List<SectionMatchResponseDTO> findSectionMatches(Long projectId, List<String> sectionKeys) {

        List<SectionMatchResponseDTO> matches = new ArrayList<>();

        if (sectionKeys == null || sectionKeys.isEmpty()) {
            return matches;
        }

        List<DocumentEntity> documentos = documentRepository
                .findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(projectId);

        for (String sectionKey : sectionKeys) {

            for (DocumentEntity documento : documentos) {

                SectionContentUtil.ExtractedSection encontrada = SectionContentUtil
                        .extractSection(documento.getContent(), sectionKey);

                if (encontrada != null) {
                    SectionMatchResponseDTO match = new SectionMatchResponseDTO();

                    match.setSectionKey(sectionKey);
                    match.setSectionTitle(encontrada.getTitle());
                    match.setSourceDocumentId(documento.getId());
                    match.setSourceDocumentTitle(documento.getTitle());
                    match.setContent(encontrada.getContent());

                    matches.add(match);
                    break;
                }
            }
        }

        return matches;
    }
}