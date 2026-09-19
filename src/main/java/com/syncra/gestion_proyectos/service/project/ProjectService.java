package com.syncra.gestion_proyectos.service.project;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncra.gestion_proyectos.dto.project.ProjectRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectResponseDTO;
import com.syncra.gestion_proyectos.dto.project.ProjectUpdateDTO;
import com.syncra.gestion_proyectos.dto.project.InstructorStatsResponseDTO;
import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.DocumentStatusEnum;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.service.kanban.KanbanColumnService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UsersRepository userRepository;
    private final ProjectMemberService projectMemberService;
    private final KanbanColumnService kanbanColumnService;
    private final DocTemplateRepository docTemplateRepository;
    private final DocumentRepository documentRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    public List<ProjectResponseDTO> getAll() {
        return toResponseList(projectRepository.findAll());
    }

    public InstructorStatsResponseDTO getInstructorStats() {
        InstructorStatsResponseDTO stats = new InstructorStatsResponseDTO();
        stats.setTotalProjects(projectRepository.count());
        stats.setInProgressProjects(projectRepository.countByStatus(ProjectStatusEnum.IN_PROGRESS));
        stats.setInReviewProjects(projectRepository.countByStatus(ProjectStatusEnum.IN_REVIEW));
        stats.setApprovedProjects(projectRepository.countByStatus(ProjectStatusEnum.APPROVED));
        stats.setRejectedProjects(projectRepository.countByStatus(ProjectStatusEnum.REJECTED));
        stats.setTotalApprentices(projectMemberRepository.countDistinctUsersByRole(RoleUserEnum.APPRENTICE));
        return stats;
    }

    public ProjectResponseDTO getById(Long id) {

        ProjectEntity entity = findOrThrow(id);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(entity.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(entity, creators);
    }

    public List<ProjectResponseDTO> getByStatus(ProjectStatusEnum status) {
        return toResponseList(projectRepository.findByStatus(status));
    }

    /**
     * Crear un nuevo proyecto y generar automaticamente sus documentos por defecto
     * a partir de las plantillas raiz publicadas.
     * 
     * @param dto       datos necesarios para crear un proyecto
     * @param createdBy id del usuario creador
     * @return información del proyecto creado
     */
    @Transactional
    public ProjectResponseDTO create(ProjectRequestDTO dto, Long createdBy) {

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getStartDate().isAfter(dto.getEndDate())) {
                throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
        }

        ProjectEntity entity = new ProjectEntity();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setGroupName(dto.getGroupName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setCreatedBy(createdBy);

        ProjectEntity saved = projectRepository.save(entity);
        kanbanColumnService.ensureCompletedColumn(saved.getId(), createdBy);

        try {
            projectMemberService.addMember(saved.getId(), createdBy);
        } catch (Exception e) {
            log.warn("No se pudo agregar al creador como miembro: {}", e.getMessage());
        }

        generarDocumentosPorDefecto(saved, createdBy);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(createdBy)).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }

    private void generarDocumentosPorDefecto(ProjectEntity project, Long createdBy) {
        List<DocTemplateEntity> rootTemplates = docTemplateRepository
                .findRootPublishedTemplates()
                .stream()
                .filter(template -> {
                    String desc = template.getDescription();
                    return desc == null || !desc.trim().toLowerCase().startsWith("plantilla hija de");
                })
                .toList();

        if (rootTemplates.isEmpty()) {
            return;
        }

        List<DocumentEntity> documentsToSave = rootTemplates.stream().map(template -> {
            DocumentEntity doc = new DocumentEntity();
            doc.setProjectId(project.getId());
            doc.setTemplateId(template.getId());
            doc.setDocumentType(DocumentTypeEnum.DOCUMENT);
            doc.setTitle(template.getTitle());
            doc.setContent(template.getDefaultContent());
            doc.setSortOrder(template.getPosition() != null ? template.getPosition().intValue() : 0);
            doc.setStatus(DocumentStatusEnum.DRAFT);
            doc.setCreatedBy(createdBy);
            return doc;
        }).toList();

        documentRepository.saveAll(documentsToSave);
    }

    @Transactional
    public ProjectResponseDTO update(Long id, ProjectUpdateDTO dto) {
        ProjectEntity entity = findOrThrow(id);
        if (dto.getName() != null)
            entity.setName(dto.getName());
        if (dto.getDescription() != null)
            entity.setDescription(dto.getDescription());
        if (dto.getGroupName() != null)
            entity.setGroupName(dto.getGroupName());
        if (dto.getStatus() != null)
            entity.setStatus(dto.getStatus());
        if (dto.getStartDate() != null)
            entity.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null)
            entity.setEndDate(dto.getEndDate());

        ProjectEntity saved = projectRepository.save(entity);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(saved.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);

        // El esquema usa claves foráneas sin cascada JPA; se eliminan primero las
        // filas dependientes para evitar errores de integridad referencial.
        jdbcTemplate.update("DELETE FROM notifications WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM activity_log WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM project_chat_messages WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM ai_usage_logs WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM ai_project_quotas WHERE project_id = ?", id);

        jdbcTemplate.update(
            "DELETE FROM private_messages WHERE conversation_id IN "
                + "(SELECT id FROM private_conversations WHERE project_id = ?)",
            id);
        jdbcTemplate.update("DELETE FROM private_conversations WHERE project_id = ?", id);

        jdbcTemplate.update(
            "DELETE FROM ai_messages WHERE conversation_id IN "
                + "(SELECT id FROM ai_conversations WHERE project_id = ?)",
            id);
        jdbcTemplate.update("DELETE FROM ai_conversations WHERE project_id = ?", id);

        jdbcTemplate.update(
            "DELETE FROM document_comments WHERE document_id IN "
                + "(SELECT id FROM documents WHERE project_id = ?)",
            id);
        jdbcTemplate.update("UPDATE documents SET parent_document_id = NULL WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM documents WHERE project_id = ?", id);

        jdbcTemplate.update(
            "DELETE FROM task_comments WHERE task_id IN "
                + "(SELECT id FROM tasks WHERE project_id = ?)",
            id);
        jdbcTemplate.update(
            "DELETE FROM task_history WHERE task_id IN "
                + "(SELECT id FROM tasks WHERE project_id = ?)",
            id);
        jdbcTemplate.update("DELETE FROM tasks WHERE project_id = ?", id);

        jdbcTemplate.update("DELETE FROM files WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM external_links WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM sprints WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM kanban_columns WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM project_members WHERE project_id = ?", id);
        jdbcTemplate.update("DELETE FROM projects WHERE id = ?", id);
    }

    @Transactional
    public ProjectResponseDTO updateStatus(Long id, ProjectStatusEnum status) {
        ProjectEntity entity = findOrThrow(id);
        entity.setStatus(status);
        ProjectEntity saved = projectRepository.save(entity);

        Map<Long, UsersEntity> creators = userRepository.findAllById(List.of(saved.getCreatedBy())).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        return toResponse(saved, creators);
    }

    private ProjectEntity findOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
    }

    private List<ProjectResponseDTO> toResponseList(List<ProjectEntity> projects) {

        List<Long> creatorIds = projects.stream()
                .map(ProjectEntity::getCreatedBy)
                .distinct()
                .toList();

        Map<Long, UsersEntity> creatorsById = userRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(UsersEntity::getId, u -> u));

        Map<Long, long[]> documentProgress = getDocumentProgress(projects);

        return projects.stream()
            .map(p -> toResponse(p, creatorsById, documentProgress.get(p.getId())))
                .toList();
    }

    private ProjectResponseDTO toResponse(ProjectEntity e, Map<Long, UsersEntity> creatorsById) {
        return toResponse(e, creatorsById, null);
        }

        private ProjectResponseDTO toResponse(ProjectEntity e, Map<Long, UsersEntity> creatorsById,
            long[] documentProgress) {
        ProjectResponseDTO r = new ProjectResponseDTO();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setDescription(e.getDescription());
        r.setGroupName(e.getGroupName());
        r.setStatus(e.getStatus());
        r.setStartDate(e.getStartDate());
        r.setEndDate(e.getEndDate());
        r.setCreatedBy(e.getCreatedBy());
        r.setCreatedAt(e.getCreatedAt());

        long totalDocuments = documentProgress == null ? 0 : documentProgress[0];
        long approvedDocuments = documentProgress == null ? 0 : documentProgress[1];
        r.setTotalDocuments(totalDocuments);
        r.setApprovedDocuments(approvedDocuments);
        r.setProgress(totalDocuments == 0 ? 0 : (int) Math.round(approvedDocuments * 100.0 / totalDocuments));

        UsersEntity creator = creatorsById.get(e.getCreatedBy());
        r.setCreatedByName(creator != null
                ? creator.getFirstName() + " " + creator.getLastName()
                : "Usuario desconocido");

        return r;
    }

    private Map<Long, long[]> getDocumentProgress(List<ProjectEntity> projects) {
        Map<Long, long[]> result = new HashMap<>();
        if (projects.isEmpty()) {
            return result;
        }

        String placeholders = projects.stream().map(project -> "?").collect(Collectors.joining(","));
        List<Long> projectIds = projects.stream().map(ProjectEntity::getId).toList();
        String sql = "SELECT project_id, COUNT(*) AS total_documents, "
                + "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_documents "
                + "FROM documents WHERE deleted_at IS NULL AND document_type = 'DOCUMENT' "
                + "AND parent_document_id IS NULL AND project_id IN (" + placeholders + ") "
                + "GROUP BY project_id";

        jdbcTemplate.queryForList(sql, projectIds.toArray()).forEach(row -> result.put(
            ((Number) row.get("project_id")).longValue(),
            new long[] {
                ((Number) row.get("total_documents")).longValue(),
                ((Number) row.get("approved_documents")).longValue()
            }));
        return result;
    }

    public List<ProjectResponseDTO> getMine(Long userId) {

        List<ProjectEntity> projects = projectMemberRepository.findByIdUserId(userId).stream()
                .map(member -> projectRepository.findById(member.getId().getProjectId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        return toResponseList(projects);
    }
}