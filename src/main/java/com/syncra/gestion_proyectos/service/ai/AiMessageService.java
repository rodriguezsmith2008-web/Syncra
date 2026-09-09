package com.syncra.gestion_proyectos.service.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.syncra.gestion_proyectos.dto.ai.AiChatResponseDTO;
import com.syncra.gestion_proyectos.dto.ai.AiHistoryItemDTO;
import com.syncra.gestion_proyectos.dto.ai.AiMessageResponseDTO;
import com.syncra.gestion_proyectos.dto.ai.AiProjectContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiServiceRequestDTO;
import com.syncra.gestion_proyectos.dto.ai.AiMemberContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiDocumentContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiActionDTO;
import com.syncra.gestion_proyectos.entity.ai.AiConversationEntity;
import com.syncra.gestion_proyectos.entity.ai.AiMessageEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.enums.AiRoleEnum;
import com.syncra.gestion_proyectos.repository.ai.AiMessageRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;
import com.syncra.gestion_proyectos.repository.kanban.KanbanColumnRepository;
import com.syncra.gestion_proyectos.repository.sprint.SprintRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.dto.task.TaskRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskMoveDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;
import com.syncra.gestion_proyectos.service.task.TaskService;
import com.syncra.gestion_proyectos.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {
	private final AiConversationService conversationService;
	private final AiMessageRepository messages;
	private final ProjectRepository projects;
	private final TaskRepository tasks;
	private final DocumentRepository documents;
	private final KanbanColumnRepository columns;
	private final SprintRepository sprints;
	private final ProjectMemberRepository members;
	private final UsersRepository users;
	private final TaskService taskService;
	private final DocumentService documentService;

	@Value("${ai.service.url}")
	private String aiServiceUrl;

	@Value("${ai.service.internal-api-key}")
	private String internalApiKey;

	public List<AiMessageResponseDTO> list(Long conversationId, Long userId) {
		conversationService.owned(conversationId, userId);
		return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(this::toResponse).toList();
	}

	@Transactional
	public AiMessageResponseDTO send(Long conversationId, Long userId, String text) {
		AiConversationEntity conversation = conversationService.owned(conversationId, userId);
		conversationService.validateProjectAccess(userId, conversation.getProjectId());
		List<AiMessageEntity> previous = messages.findByConversationIdOrderByCreatedAtAsc(conversationId);

		AiMessageEntity userMessage = saveMessage(conversationId, AiRoleEnum.USER, text.trim());
		AiServiceRequestDTO request = new AiServiceRequestDTO();
		request.setMessage(userMessage.getContent());
		request.setHistory(previous.stream().skip(Math.max(0, previous.size() - 10))
				.map(m -> new AiHistoryItemDTO(m.getRole().name(), m.getContent())).toList());
		request.setContext(buildContext(conversation.getProjectId()));
		if (request.getContext() != null) {
			users.findById(userId).ifPresent(user -> {
				request.getContext().setCurrentUserId(userId);
				request.getContext().setCurrentUserName(user.getFirstName() + " " + user.getLastName());
			});
		}

		AiChatResponseDTO response;
		try {
			response = RestClient.builder().baseUrl(aiServiceUrl.trim()).build().post().uri("/ai/chat")
					.header("X-Internal-Api-Key", internalApiKey)
					.body(request).retrieve().body(AiChatResponseDTO.class);
		} catch (RestClientException ex) {
			log.error("Error llamando al microservicio IA en {}: {}", aiServiceUrl, ex.getMessage());
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_GATEWAY,
					"El asistente no está disponible en este momento");
		}
		if (response == null || response.getReply() == null || response.getReply().isBlank()) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_GATEWAY,
					"El asistente devolvió una respuesta inválida");
		}

		List<String> executedActions = executeActions(response.getActions(), conversation.getProjectId(), userId);
		AiMessageEntity assistant = saveMessage(conversationId, AiRoleEnum.ASSISTANT, response.getReply());
		AiMessageResponseDTO result = toResponse(assistant);
		result.setSuggestedCard(response.getSuggestedCard());
		result.setExecutedActions(executedActions);
		return result;
	}

	private List<String> executeActions(List<AiActionDTO> actions, Long projectId, Long userId) {
		if (actions == null || actions.isEmpty()) return List.of();
		if (projectId == null) throw new IllegalArgumentException("Selecciona un proyecto para ejecutar acciones");
		List<String> executed = new java.util.ArrayList<>();
		for (AiActionDTO action : actions) {
			if (action == null || action.getType() == null) continue;
			String type = action.getType().trim().toUpperCase(java.util.Locale.ROOT);
			if (action.getProjectId() != null && !projectId.equals(action.getProjectId())) {
				throw new org.springframework.security.access.AccessDeniedException("La acción apunta a otro proyecto");
			}
			switch (type) {
			case "CREATE_TASK" -> {
				validateColumn(projectId, action.getColumnId());
				validateMember(projectId, action.getAssignedTo());
				TaskRequestDTO dto = new TaskRequestDTO();
				dto.setColumnId(action.getColumnId()); dto.setSprintId(action.getSprintId());
				dto.setTitle(required(action.getTitle(), "La tarea necesita título"));
				dto.setDescription(action.getDescription()); dto.setDueDate(action.getDueDate());
				dto.setAssignedTo(action.getAssignedTo()); dto.setColor(action.getColor());
				if (dto.getAssignedTo() == null) dto.setAssignedTo(userId);
				taskService.create(projectId, userId, dto); executed.add("Tarea creada: " + dto.getTitle());
			}
			case "UPDATE_TASK", "ASSIGN_TASK" -> {
				var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow();
				validateProject(task.getProjectId(), projectId); validateMember(projectId, action.getAssignedTo());
				TaskRequestDTO dto = taskRequest(action, task); taskService.update(task.getId(), userId, dto);
				executed.add("Tarea actualizada: " + task.getTitle());
			}
			case "MOVE_TASK" -> {
				var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow();
				validateProject(task.getProjectId(), projectId); validateColumn(projectId, action.getColumnId());
				TaskMoveDTO dto = new TaskMoveDTO(); dto.setColumnId(action.getColumnId()); dto.setPosition(action.getPosition() == null ? 0L : action.getPosition());
				taskService.move(task.getId(), userId, dto); executed.add("Tarea movida: " + task.getTitle());
			}
			case "DELETE_TASK" -> {
				var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow(); validateProject(task.getProjectId(), projectId);
				taskService.delete(task.getId()); executed.add("Tarea eliminada: " + task.getTitle());
			}
			case "CREATE_DOCUMENT" -> {
				DocumentRequestDTO dto = new DocumentRequestDTO(); dto.setTitle(required(action.getTitle(), "El documento necesita título"));
				dto.setDocumentType(parseDocumentType(action.getDocumentType())); dto.setSprintId(action.getSprintId());
				var created = documentService.create(projectId, dto, userId);
				if (action.getContent() != null) { DocumentUpdateDTO update = new DocumentUpdateDTO(); update.setContent(action.getContent()); documentService.update(created.getId(), update, userId); }
				executed.add("Documento creado: " + created.getTitle());
			}
			case "UPDATE_DOCUMENT" -> {
				var document = documents.findByIdAndDeletedAtIsNull(requiredId(action.getDocumentId(), "Falta document_id")).orElseThrow(); validateProject(document.getProjectId(), projectId);
				DocumentUpdateDTO dto = new DocumentUpdateDTO(); dto.setTitle(action.getTitle()); dto.setContent(action.getContent()); dto.setSprintId(action.getSprintId());
				documentService.update(document.getId(), dto, userId); executed.add("Documento actualizado: " + document.getTitle());
			}
			case "DELETE_DOCUMENT" -> {
				var document = documents.findByIdAndDeletedAtIsNull(requiredId(action.getDocumentId(), "Falta document_id")).orElseThrow(); validateProject(document.getProjectId(), projectId);
				documentService.delete(document.getId()); executed.add("Documento eliminado: " + document.getTitle());
			}
			default -> log.warn("Acción IA no soportada: {}", type);
			}
		}
		return executed;
	}

	private TaskRequestDTO taskRequest(AiActionDTO action, com.syncra.gestion_proyectos.entity.task.TaskEntity task) {
		TaskRequestDTO dto = new TaskRequestDTO();
		dto.setTitle(action.getTitle() == null ? task.getTitle() : action.getTitle());
		dto.setDescription(action.getDescription() == null ? task.getDescription() : action.getDescription());
		dto.setDueDate(action.getDueDate() == null ? task.getDueDate() : action.getDueDate());
		dto.setAssignedTo(action.getAssignedTo() == null ? task.getAssignedTo() : action.getAssignedTo());
		dto.setSprintId(action.getSprintId() == null ? task.getSprintId() : action.getSprintId());
		dto.setColor(action.getColor() == null ? task.getColor() : action.getColor());
		return dto;
	}
	private String required(String value, String message) { if (value == null || value.isBlank()) throw new IllegalArgumentException(message); return value.trim(); }
	private Long requiredId(Long value, String message) { if (value == null) throw new IllegalArgumentException(message); return value; }
	private DocumentTypeEnum parseDocumentType(String value) { return "MEETING_MINUTES".equalsIgnoreCase(value) ? DocumentTypeEnum.MEETING_MINUTES : DocumentTypeEnum.DOCUMENT; }
	private void validateProject(Long actual, Long expected) { if (!expected.equals(actual)) throw new org.springframework.security.access.AccessDeniedException("El recurso pertenece a otro proyecto"); }
	private void validateColumn(Long projectId, Long columnId) { if (columnId == null || columns.findById(columnId).filter(column -> projectId.equals(column.getProjectId())).isEmpty()) throw new IllegalArgumentException("La columna no pertenece al proyecto"); }
	private void validateMember(Long projectId, Long userId) { if (userId != null && !members.existsByIdProjectIdAndIdUserId(projectId, userId)) throw new org.springframework.security.access.AccessDeniedException("El responsable no pertenece al proyecto"); }

	private AiMessageEntity saveMessage(Long conversationId, AiRoleEnum role, String content) {
		AiMessageEntity entity = new AiMessageEntity();
		entity.setConversationId(conversationId);
		entity.setRole(role);
		entity.setContent(content);
		return messages.save(entity);
	}

	private AiProjectContextDTO buildContext(Long projectId) {
		if (projectId == null) return null;
		ProjectEntity project = projects.findById(projectId).orElse(null);
		if (project == null) return null;
		AiProjectContextDTO context = new AiProjectContextDTO();
		context.setProjectId(project.getId());
		context.setProjectName(project.getName());
		context.setProjectStatus(project.getStatus() == null ? null : project.getStatus().name());
		context.setPendingTasksSummary(tasks.findByProjectIdOrderByColumnIdAscPositionAsc(projectId).stream()
				.filter(task -> task.getDueDate() != null).limit(20)
				.map(task -> task.getTitle() + " (vence " + task.getDueDate() + ")")
				.collect(java.util.stream.Collectors.joining(", ")));
			context.setColumns(columns.findByProjectIdOrderByPositionAsc(projectId).stream()
				.map(column -> new com.syncra.gestion_proyectos.dto.ai.AiColumnContextDTO(
					column.getId(), column.getName(), column.getPosition())).toList());
			context.setTasks(tasks.findByProjectIdOrderByColumnIdAscPositionAsc(projectId).stream()
				.map(task -> new com.syncra.gestion_proyectos.dto.ai.AiTaskContextDTO(
					task.getId(), task.getTitle(), limit(task.getDescription(), 1200), task.getColumnId(),
					task.getSprintId(), task.getDueDate(), task.getAssignedTo())).toList());
			context.setSprints(sprints.findByProjectId(projectId).stream()
				.map(sprint -> new com.syncra.gestion_proyectos.dto.ai.AiSprintContextDTO(
					sprint.getId(), sprint.getName(), sprint.getStartDate(), sprint.getEndDate(),
					sprint.getStatus() == null ? null : sprint.getStatus().name())).toList());
			context.setMembers(members.findByIdProjectId(projectId).stream().map(ProjectMemberEntity::getId)
				.map(id -> users.findById(id.getUserId()).map(user -> new AiMemberContextDTO(
					user.getId(), user.getFirstName() + " " + user.getLastName(),
					user.getRole() == null ? null : user.getRole().name())).orElse(null))
				.filter(java.util.Objects::nonNull).toList());
			context.setDocuments(documents.findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(projectId).stream()
				.map(document -> new AiDocumentContextDTO(document.getId(), document.getTitle(),
					document.getDocumentType() == null ? null : document.getDocumentType().name(),
					document.getParentDocumentId(), document.getStatus() == null ? null : document.getStatus().name(),
					limit(document.getContent(), 6000), document.getMeetingType())).toList());
		return context;
	}

		    private String limit(String value, int max) {
			if (value == null || value.length() <= max) return value;
			return value.substring(0, max) + "\n[contenido truncado]";
		    }

	private AiMessageResponseDTO toResponse(AiMessageEntity entity) {
		AiMessageResponseDTO dto = new AiMessageResponseDTO();
		dto.setId(entity.getId());
		dto.setConversationId(entity.getConversationId());
		dto.setRole(entity.getRole());
		dto.setContent(entity.getContent());
		dto.setCreatedAt(entity.getCreatedAt());
		return dto;
	}
}
