package com.syncra.gestion_proyectos.service.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncra.gestion_proyectos.dto.ai.AiChatResponseDTO;
import com.syncra.gestion_proyectos.dto.ai.AiHistoryItemDTO;
import com.syncra.gestion_proyectos.dto.ai.AiMessageResponseDTO;
import com.syncra.gestion_proyectos.dto.ai.AiProjectContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiQuotaStatusDTO;
import com.syncra.gestion_proyectos.dto.ai.AiServiceRequestDTO;
import com.syncra.gestion_proyectos.dto.ai.AiMemberContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiDocumentContextDTO;
import com.syncra.gestion_proyectos.dto.ai.AiActionDTO;
import com.syncra.gestion_proyectos.dto.ai.AiToolCallDTO;
import com.syncra.gestion_proyectos.entity.ai.AiConversationEntity;
import com.syncra.gestion_proyectos.entity.ai.AiMessageEntity;
import com.syncra.gestion_proyectos.entity.ai.AiUsageLogEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.enums.AiModelTierEnum;
import com.syncra.gestion_proyectos.enums.AiRoleEnum;
import com.syncra.gestion_proyectos.repository.ai.AiMessageRepository;
import com.syncra.gestion_proyectos.repository.ai.AiUsageLogRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.task.TaskRepository;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;
import com.syncra.gestion_proyectos.repository.kanban.KanbanColumnRepository;
import com.syncra.gestion_proyectos.repository.sprint.SprintRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.dto.task.TaskRequestDTO;
import com.syncra.gestion_proyectos.dto.task.TaskResponseDTO;
import com.syncra.gestion_proyectos.dto.task.TaskMoveDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentRequestDTO;
import com.syncra.gestion_proyectos.dto.document.DocumentUpdateDTO;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;
import com.syncra.gestion_proyectos.service.task.TaskService;
import com.syncra.gestion_proyectos.service.document.DocumentService;
import com.syncra.gestion_proyectos.dto.KanbaColumn.KanbaColumRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {
	private final AiConversationService conversationService;
	private final AiMessageRepository messages;
	private final AiUsageLogRepository usageLogs;
	private final AiQuotaService quotaService;
	private final ProjectRepository projects;
	private final TaskRepository tasks;
	private final DocumentRepository documents;
	private final DocTemplateRepository docTemplates;
	private final KanbanColumnRepository columns;
	private final SprintRepository sprints;
	private final ProjectMemberRepository members;
	private final UsersRepository users;
	private final TaskService taskService;
	private final DocumentService documentService;
	private final com.syncra.gestion_proyectos.service.kanban.KanbanColumnService kanbanColumnService;

	@Value("${ai.service.url}")
	private String aiServiceUrl;

	@Value("${ai.service.internal-api-key}")
	private String internalApiKey;

	private static final int HISTORY_WINDOW = 6;

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
		AiModelTierEnum selectedTier = selectAvailableTier(conversation.getProjectId(), null);
		if (selectedTier == null) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
					"El límite diario de IA de este proyecto se ha agotado. Se renovará en el siguiente periodo.");
		}

		AiMessageEntity userMessage = saveMessage(conversationId, AiRoleEnum.USER, text.trim());
		String userText = userMessage.getContent();
		boolean trivial = isTrivialMessage(userText, previous);

		AiServiceRequestDTO request = new AiServiceRequestDTO();
		request.setMessage(userText);
		request.setHistory(previous.stream().skip(Math.max(0, previous.size() - HISTORY_WINDOW))
				.map(m -> new AiHistoryItemDTO(m.getRole().name(), m.getContent())).toList());
		request.setContext(trivial ? null : buildContext(conversation.getProjectId()));
		request.setModel(selectedTier.name().toLowerCase(java.util.Locale.ROOT));
		request.setTools(trivial ? java.util.List.of() : buildToolDefinitions());
		if (!trivial && request.getContext() != null) {
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
		} catch (org.springframework.web.client.RestClientResponseException ex) {
			String aiMessage = extractAiServiceError(ex.getResponseBodyAsString());
			org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus
					.resolve(ex.getStatusCode().value());
			if (status == null)
				status = org.springframework.http.HttpStatus.BAD_GATEWAY;
			log.error("Microservicio de IA respondió con HTTP {}: {}", ex.getStatusCode(), aiMessage);
			throw new org.springframework.web.server.ResponseStatusException(
					status,
					aiMessage != null && !aiMessage.isBlank() ? aiMessage
							: "El asistente no está disponible en este momento");
		} catch (RestClientException ex) {
			log.error("Error llamando al microservicio IA en {}: {}", aiServiceUrl, ex.getMessage());
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_GATEWAY,
					"El asistente no está disponible en este momento");
		}

		if (Boolean.FALSE.equals(response.getSuccess())) {
			String aiMessage = response.getStatusMessage() != null ? response.getStatusMessage()
					: "El asistente no está disponible en este momento";
			org.springframework.http.HttpStatus status = response.getHttpStatus() != null
					? org.springframework.http.HttpStatus.valueOf(response.getHttpStatus())
					: org.springframework.http.HttpStatus.BAD_GATEWAY;
			throw new org.springframework.web.server.ResponseStatusException(status, aiMessage);
		}
		if (response.getReply() == null || response.getReply().isBlank()) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_GATEWAY,
					"El asistente devolvió una respuesta inválida");
		}

		List<String> executedActions = new java.util.ArrayList<>();
		int toolCallRounds = 0;
		long tokensAccumulated = extractTotalTokens(response.getUsage());

		while (response.getToolCalls() != null && !response.getToolCalls().isEmpty() && toolCallRounds < 5) {
			toolCallRounds++;
			List<String> toolExecution = executeToolCalls(response.getToolCalls(), conversation.getProjectId(), userId);
			executedActions.addAll(toolExecution);
			if (toolExecution.isEmpty())
				break;

			String followUpMessage =
				"El usuario pidió originalmente:\n\"" + userText + "\"\n\n" +
				"He ejecutado estas herramientas del backend con los siguientes resultados:\n" +
				String.join("\n", toolExecution) +
				"\n\nINSTRUCCIÓN OBLIGATORIA: continúa trabajando en la petición original del usuario. " +
				"Si para completarla necesitas ejecutar más herramientas, hazlo AHORA llamándolas. " +
				"Solo responde con una síntesis al usuario cuando la tarea esté realmente completa.";

			AiServiceRequestDTO followUp = new AiServiceRequestDTO();
			followUp.setMessage(followUpMessage);
			followUp.setHistory(previous.stream().skip(Math.max(0, previous.size() - HISTORY_WINDOW))
					.map(m -> new AiHistoryItemDTO(m.getRole().name(), m.getContent())).toList());
			followUp.setContext(null); // el modelo ya vio el contexto en la primera llamada
			followUp.setModel(request.getModel());
			followUp.setTools(buildToolDefinitions());

			try {
				AiChatResponseDTO followUpResponse = RestClient.builder().baseUrl(aiServiceUrl.trim()).build()
						.post().uri("/ai/chat")
						.header("X-Internal-Api-Key", internalApiKey)
						.body(followUp).retrieve().body(AiChatResponseDTO.class);
				if (followUpResponse != null && followUpResponse.getReply() != null
						&& !followUpResponse.getReply().isBlank()) {
					tokensAccumulated += extractTotalTokens(followUpResponse.getUsage());
					response.setReply(followUpResponse.getReply());
					response.setSuggestedCard(followUpResponse.getSuggestedCard());
					response.setActions(followUpResponse.getActions());
					response.setToolCalls(followUpResponse.getToolCalls());
					if (followUpResponse.getUsage() != null) {
						response.setUsage(followUpResponse.getUsage());
					}
					if (followUpResponse.getToolCalls() == null || followUpResponse.getToolCalls().isEmpty())
						break;
				} else {
					break;
				}
			} catch (Exception ex) {
				log.warn("No se pudo cerrar el seguimiento del tool call del asistente: {}", ex.getMessage());
				break;
			}
		}

		executedActions.addAll(executeActions(response.getActions(), conversation.getProjectId(), userId));

		// Nunca exponer el placeholder interno del ai-service al usuario.
		if (response.getReply() == null || response.getReply().isBlank()
				|| response.getReply().equals("He recibido la necesidad de ejecutar una herramienta del backend.")) {
			if (!executedActions.isEmpty()) {
				response.setReply("Ejecuté estas acciones:\n- " + String.join("\n- ", executedActions)
						+ "\n\nNo pude generar una respuesta final. Prueba de nuevo o reformula la petición.");
			} else {
				response.setReply("No pude completar la operación. Intenta de nuevo.");
			}
		}

		AiMessageEntity assistant = saveMessage(conversationId, AiRoleEnum.ASSISTANT, response.getReply());
		AiMessageResponseDTO result = toResponse(assistant);
		result.setSuggestedCard(response.getSuggestedCard());
		result.setExecutedActions(executedActions);
		result.setModel(response.getModel() == null ? request.getModel() : response.getModel());
		result.setLevel(response.getLevel() == null ? modelLevel(selectedTier) : response.getLevel());
		result.setFallback(Boolean.TRUE.equals(response.getFallback()));
		result.setStatusMessage(response.getStatusMessage() == null
				? quotaService.statusMessageForTier(selectedTier)
				: response.getStatusMessage());
		result.setQuota(response.getQuota() == null
				? quotaStatusForProject(conversation.getProjectId(), selectedTier)
				: response.getQuota());
		recordUsage(conversation.getProjectId(), userId, selectedTier, result.getModel(), tokensAccumulated);
		return result;
	}

	private boolean isTrivialMessage(String text, List<AiMessageEntity> previous) {
		if (text == null || text.isBlank())
			return true;

		// Regla 1: si el último mensaje del asistente fue una pregunta o propuesta,
		// cualquier respuesta del usuario es continuación → NO trivial.
		if (previous != null && !previous.isEmpty()) {
			for (int i = previous.size() - 1; i >= 0; i--) {
				AiMessageEntity m = previous.get(i);
				if (m.getRole() != AiRoleEnum.ASSISTANT)
					continue;
				String last = m.getContent() == null ? "" : m.getContent().toLowerCase(java.util.Locale.ROOT);
				boolean assistantProposedAction = last.contains("?")
						|| last.contains("¿")
						|| last.contains("confirma")
						|| last.contains("procedo")
						|| last.contains("voy a ")
						|| last.contains("dime si")
						|| last.contains("quieres que")
						|| last.contains("necesitas");
				if (assistantProposedAction)
					return false;
				break;
			}
		}

		String trimmed = text.trim();
		if (trimmed.length() > 60)
			return false;
		String normalized = trimmed.toLowerCase(java.util.Locale.ROOT);
		String[] keywords = {
				"crea", "crear", "documento", "acta", "tarea", "proyecto", "lista",
				"mueve", "mover", "asigna", "asignar", "elimina", "actualiza",
				"resume", "analiza", "miembro", "columna", "tablero", "sprint", "board"
		};
		for (String keyword : keywords) {
			if (normalized.contains(keyword))
				return false;
		}
		return true;
	}

	private long extractTotalTokens(java.util.Map<String, Object> usage) {
		if (usage == null)
			return 0L;
		Object raw = usage.get("total_tokens");
		if (raw instanceof Number n)
			return n.longValue();
		if (raw instanceof String s) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return 0L;
	}

	private List<String> executeToolCalls(List<AiToolCallDTO> toolCalls, Long projectId, Long userId) {
		if (toolCalls == null || toolCalls.isEmpty())
			return List.of();
		List<String> executed = new java.util.ArrayList<>();

		for (AiToolCallDTO call : toolCalls) {
			if (call == null || call.getFunction() == null || call.getFunction().getName() == null)
				continue;
			String functionName = call.getFunction().getName().trim();
			java.util.Map<String, Object> args = call.getFunction().getArguments() == null
					? java.util.Map.of()
					: call.getFunction().getArguments();

			try {
				switch (functionName) {
					case "get_project" -> {
						Long targetProjectId = coalesceLong(args.get("project_id"), projectId);
						validateProject(targetProjectId, projectId);
						ProjectEntity project = projects.findById(targetProjectId)
								.orElseThrow(() -> new IllegalArgumentException("El proyecto no existe"));
						executed.add("Proyecto: " + project.getName() + " (id=" + project.getId() + ")");
					}
					case "get_project_members" -> {
						Long targetProjectId = coalesceLong(args.get("project_id"), projectId);
						validateProject(targetProjectId, projectId);
						List<String> lines = new java.util.ArrayList<>();
						for (ProjectMemberEntity member : members.findByIdProjectId(targetProjectId)) {
							Long memberUserId = member.getId().getUserId();
							users.findById(memberUserId).ifPresent(u -> lines.add(
									"id=" + u.getId() + " | " + u.getFirstName() + " " + u.getLastName()
											+ " | rol=" + (u.getRole() == null ? "sin rol" : u.getRole().name())));
						}
						if (lines.isEmpty())
							executed.add("El proyecto no tiene miembros registrados.");
						else
							executed.add("Miembros del proyecto (" + lines.size() + "):\n" + String.join("\n", lines));
					}
					case "get_tasks" -> {
						Long targetProjectId = coalesceLong(args.get("project_id"), projectId);
						validateProject(targetProjectId, projectId);
						var taskList = tasks.findByProjectIdOrderByColumnIdAscPositionAsc(targetProjectId);
						if (taskList.isEmpty())
							executed.add("El proyecto no tiene tareas.");
						else {
							StringBuilder sb = new StringBuilder("Tareas (" + taskList.size() + "):");
							for (var task : taskList) {
								sb.append("\n- id=").append(task.getId())
										.append(" | ").append(task.getTitle())
										.append(" | columna=").append(task.getColumnId())
										.append(" | responsable=")
										.append(task.getAssignedTo() == null ? "sin asignar" : task.getAssignedTo())
										.append(" | vence=")
										.append(task.getDueDate() == null ? "sin fecha" : task.getDueDate());
							}
							executed.add(sb.toString());
						}
					}
					case "create_task" -> {
						TaskRequestDTO dto = new TaskRequestDTO();
						dto.setTitle(required(String.valueOf(args.getOrDefault("title", "")).trim(),
								"El título es obligatorio"));
						dto.setDescription(
								args.get("description") == null ? null : String.valueOf(args.get("description")));
						dto.setColumnId(
								args.get("column_id") == null ? null : coalesceLong(args.get("column_id"), null));
						dto.setSprintId(
								args.get("sprint_id") == null ? null : coalesceLong(args.get("sprint_id"), null));
						dto.setAssignedTo(
								args.get("assigned_to") == null ? null : coalesceLong(args.get("assigned_to"), null));
						dto.setDueDate(args.get("due_date") == null ? null
								: java.time.LocalDate.parse(String.valueOf(args.get("due_date"))));
						dto.setColor(args.get("color") == null ? null : String.valueOf(args.get("color")));
						if (dto.getColumnId() != null)
							validateColumn(projectId, dto.getColumnId());
						if (dto.getAssignedTo() != null)
							validateMember(projectId, dto.getAssignedTo());
						TaskResponseDTO created = taskService.create(projectId, userId, dto);
						executed.add("Tarea creada: " + created.getTitle() + " (id=" + created.getId() + ")");
					}
					case "list_documents" -> {
						var list = documents.findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(projectId);
						if (list.isEmpty())
							executed.add("El proyecto no tiene documentos ni actas.");
						else {
							StringBuilder sb = new StringBuilder("Documentos (" + list.size() + "):");
							for (var doc : list) {
								sb.append("\n- id=").append(doc.getId())
										.append(" | ").append(doc.getTitle())
										.append(" | tipo=").append(doc.getDocumentType())
										.append(" | estado=").append(doc.getStatus());
							}
							executed.add(sb.toString());
						}
					}
					case "get_document" -> {
						Long documentId = coalesceLong(args.get("document_id"), null);
						if (documentId == null)
							throw new IllegalArgumentException("Falta document_id");
						var document = documents.findByIdAndDeletedAtIsNull(documentId)
								.orElseThrow(() -> new IllegalArgumentException("El documento no existe"));
						validateProject(document.getProjectId(), projectId);
						executed.add("Documento: " + document.getTitle()
								+ " (id=" + document.getId() + ", tipo=" + document.getDocumentType() + ")"
								+ "\nContenido:\n" + limit(document.getContent(), 2000));
					}
					case "create_document" -> {
						String title = required(String.valueOf(args.getOrDefault("title", "")).trim(),
								"El documento necesita título");
						String typeRaw = args.get("document_type") == null ? "DOCUMENT"
								: String.valueOf(args.get("document_type"));

						DocumentRequestDTO dto = new DocumentRequestDTO();
						dto.setTitle(title);
						dto.setDocumentType(parseDocumentType(typeRaw));
						dto.setSprintId(
								args.get("sprint_id") == null ? null : coalesceLong(args.get("sprint_id"), null));
						if (args.get("meeting_type") != null)
							dto.setMeetingType(String.valueOf(args.get("meeting_type")));

						// Resolver plantilla por código o por nombre (case-insensitive, tildes-insensitive)
						String templateCode = args.get("template_code") == null ? null
								: String.valueOf(args.get("template_code")).trim();
						String templateName = args.get("template_name") == null ? null
								: String.valueOf(args.get("template_name")).trim();
						Long resolvedTemplateId = null;
						String resolvedTemplateLabel = null;

						if (templateCode != null && !templateCode.isBlank()) {
							var byCode = docTemplates.findByCode(templateCode);
							if (byCode.isPresent()) {
								resolvedTemplateId = byCode.get().getId();
								resolvedTemplateLabel = byCode.get().getTitle() + " (" + byCode.get().getCode() + ")";
							}
						}
						if (resolvedTemplateId == null && templateName != null && !templateName.isBlank()) {
							String needle = normalizeForMatch(templateName);
							var all = docTemplates.findAll();
							for (var t : all) {
								if (t.getTitle() != null && normalizeForMatch(t.getTitle()).equals(needle)) {
									resolvedTemplateId = t.getId();
									resolvedTemplateLabel = t.getTitle() + " (" + t.getCode() + ")";
									break;
								}
							}
							if (resolvedTemplateId == null) {
								for (var t : all) {
									if (t.getTitle() != null && normalizeForMatch(t.getTitle()).contains(needle)) {
										resolvedTemplateId = t.getId();
										resolvedTemplateLabel = t.getTitle() + " (" + t.getCode() + ")";
										break;
									}
								}
							}
						}
						if (resolvedTemplateId != null) {
							dto.setTemplateId(resolvedTemplateId);
						}

						var created = documentService.create(projectId, dto, userId);

						if (args.get("content") != null && !String.valueOf(args.get("content")).isBlank()) {
							DocumentUpdateDTO update = new DocumentUpdateDTO();
							update.setContent(String.valueOf(args.get("content")));
							documentService.update(created.getId(), update, userId);
						}

						String extra = resolvedTemplateLabel != null
								? ", plantilla=" + resolvedTemplateLabel
								: ((templateName != null || templateCode != null) ? ", plantilla NO encontrada" : "");
						executed.add("Documento creado: " + created.getTitle() + " (id=" + created.getId() + ", tipo="
								+ typeRaw + extra + ")");
					}
					case "get_board_columns" -> {
						Long targetProjectId = coalesceLong(args.get("project_id"), projectId);
						validateProject(targetProjectId, projectId);
						var cols = columns.findByProjectIdOrderByPositionAsc(targetProjectId);
						if (cols.isEmpty())
							executed.add("El tablero no tiene columnas.");
						else {
							StringBuilder sb = new StringBuilder("Columnas (" + cols.size() + "):");
							for (var col : cols) {
								sb.append("\n- id=").append(col.getId())
										.append(" | ").append(col.getName())
										.append(" | posición=").append(col.getPosition())
										.append(" | color=").append(col.getColor() == null ? "—" : col.getColor());
							}
							executed.add(sb.toString());
						}
					}
					case "create_board_column" -> {
						String name = required(String.valueOf(args.getOrDefault("name", "")).trim(),
								"La columna necesita nombre");
						var existing = columns.findByProjectIdOrderByPositionAsc(projectId).stream()
								.filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(name))
								.findFirst();
						if (existing.isPresent()) {
							var col = existing.get();
							executed.add("Columna reutilizada: " + col.getName() + " (id=" + col.getId() + ")");
						} else {
							KanbaColumRequestDTO colDto = new KanbaColumRequestDTO();
							colDto.setName(name);
							colDto.setColor(args.get("color") == null ? null : String.valueOf(args.get("color")));
							var message = kanbanColumnService.create(projectId, colDto, userId);
							if (message.getData() != null) {
								executed.add("Columna creada: " + message.getData().getName() + " (id="
										+ message.getData().getId() + ")");
							} else {
								executed.add("No se pudo crear la columna: " + message.getMessage());
							}
						}
					}
					default -> executed.add("Herramienta no soportada por Syncra: " + functionName);
				}
			} catch (Exception ex) {
				executed.add("Error al ejecutar herramienta " + functionName + ": " + ex.getMessage());
				log.warn("La herramienta {} falló en el backend: {}", functionName, ex.getMessage());
			}
		}
		return executed;
	}

	private Long coalesceLong(Object value, Long fallback) {
		if (value == null)
			return fallback;
		if (value instanceof Number number)
			return number.longValue();
		if (value instanceof String raw) {
			String trimmed = raw.trim();
			if (trimmed.isBlank())
				return fallback;
			return Long.parseLong(trimmed);
		}
		return fallback;
	}

	private List<String> executeActions(List<AiActionDTO> actions, Long projectId, Long userId) {
		if (actions == null || actions.isEmpty())
			return List.of();
		if (projectId == null)
			throw new IllegalArgumentException("Selecciona un proyecto para ejecutar acciones");
		List<String> executed = new java.util.ArrayList<>();
		for (AiActionDTO action : actions) {
			if (action == null || action.getType() == null)
				continue;
			String type = action.getType().trim().toUpperCase(java.util.Locale.ROOT);
			if (action.getProjectId() != null && !projectId.equals(action.getProjectId())) {
				throw new org.springframework.security.access.AccessDeniedException("La acción apunta a otro proyecto");
			}
			switch (type) {
				case "CREATE_TASK" -> {
					validateColumn(projectId, action.getColumnId());
					validateMember(projectId, action.getAssignedTo());
					TaskRequestDTO dto = new TaskRequestDTO();
					dto.setColumnId(action.getColumnId());
					dto.setSprintId(action.getSprintId());
					dto.setTitle(required(action.getTitle(), "La tarea necesita título"));
					dto.setDescription(action.getDescription());
					dto.setDueDate(action.getDueDate());
					dto.setAssignedTo(action.getAssignedTo());
					dto.setColor(action.getColor());
					if (dto.getAssignedTo() == null)
						dto.setAssignedTo(userId);
					taskService.create(projectId, userId, dto);
					executed.add("Tarea creada: " + dto.getTitle());
				}
				case "UPDATE_TASK", "ASSIGN_TASK" -> {
					var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow();
					validateProject(task.getProjectId(), projectId);
					validateMember(projectId, action.getAssignedTo());
					TaskRequestDTO dto = taskRequest(action, task);
					taskService.update(task.getId(), userId, dto);
					executed.add("Tarea actualizada: " + task.getTitle());
				}
				case "MOVE_TASK" -> {
					var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow();
					validateProject(task.getProjectId(), projectId);
					validateColumn(projectId, action.getColumnId());
					TaskMoveDTO dto = new TaskMoveDTO();
					dto.setColumnId(action.getColumnId());
					dto.setPosition(action.getPosition() == null ? 0L : action.getPosition());
					taskService.move(task.getId(), userId, dto);
					executed.add("Tarea movida: " + task.getTitle());
				}
				case "DELETE_TASK" -> {
					var task = tasks.findById(requiredId(action.getTaskId(), "Falta task_id")).orElseThrow();
					validateProject(task.getProjectId(), projectId);
					taskService.delete(task.getId());
					executed.add("Tarea eliminada: " + task.getTitle());
				}
				case "CREATE_DOCUMENT" -> {
					DocumentRequestDTO dto = new DocumentRequestDTO();
					dto.setTitle(required(action.getTitle(), "El documento necesita título"));
					dto.setDocumentType(parseDocumentType(action.getDocumentType()));
					dto.setSprintId(action.getSprintId());
					var created = documentService.create(projectId, dto, userId);
					if (action.getContent() != null) {
						DocumentUpdateDTO update = new DocumentUpdateDTO();
						update.setContent(action.getContent());
						documentService.update(created.getId(), update, userId);
					}
					executed.add("Documento creado: " + created.getTitle());
				}
				case "UPDATE_DOCUMENT" -> {
					var document = documents
							.findByIdAndDeletedAtIsNull(requiredId(action.getDocumentId(), "Falta document_id"))
							.orElseThrow();
					validateProject(document.getProjectId(), projectId);
					DocumentUpdateDTO dto = new DocumentUpdateDTO();
					dto.setTitle(action.getTitle());
					dto.setContent(action.getContent());
					dto.setSprintId(action.getSprintId());
					documentService.update(document.getId(), dto, userId);
					executed.add("Documento actualizado: " + document.getTitle());
				}
				case "DELETE_DOCUMENT" -> {
					var document = documents
							.findByIdAndDeletedAtIsNull(requiredId(action.getDocumentId(), "Falta document_id"))
							.orElseThrow();
					validateProject(document.getProjectId(), projectId);
					documentService.delete(document.getId());
					executed.add("Documento eliminado: " + document.getTitle());
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

	private List<java.util.Map<String, Object>> buildToolDefinitions() {
		return java.util.List.of(
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_project",
						"description", "Consulta el proyecto actual y su contexto.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_project_members",
						"description", "Lista los miembros del proyecto para identificar responsables.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_tasks",
						"description", "Lista tareas del proyecto para revisar estado, fechas y responsables.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "list_documents",
						"description", "Lista documentos y actas para identificar contenido relevante.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_document",
						"description", "Consulta un documento concreto por id.",
						"parameters", java.util.Map.of("type", "object",
								"properties",
								java.util.Map.of("project_id", java.util.Map.of("type", "integer"), "document_id",
										java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id", "document_id"), "additionalProperties",
								false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "create_document",
						"description", "Crea un documento o acta real en la base de datos del proyecto. Si el usuario menciona una plantilla específica, pásala en template_name (o template_code si la conoces). Si es un acta de reunión, usa document_type=MEETING_MINUTES.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"title", java.util.Map.of("type", "string"),
										"document_type", java.util.Map.of("type", "string"),
										"content", java.util.Map.of("type", "string"),
										"meeting_type", java.util.Map.of("type", "string"),
										"template_code", java.util.Map.of("type", "string",
												"description", "Código de la plantilla (ej: PT-AR-01). Preferido si lo conoces."),
										"template_name", java.util.Map.of("type", "string",
												"description", "Nombre de la plantilla si no conoces el código (ej: Pruebas).")),
								"required", java.util.List.of("project_id", "title"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "create_task",
						"description", "Crea una tarea real en el tablero del proyecto.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"title", java.util.Map.of("type", "string"),
										"description", java.util.Map.of("type", "string"),
										"due_date", java.util.Map.of("type", "string", "format", "date"),
										"assigned_to", java.util.Map.of("type", "integer"),
										"column_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id", "title"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_board_columns",
						"description", "Lista columnas del tablero para reutilizar o crear estados adecuados.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", java.util.List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "create_board_column",
						"description", "Crea una columna en el proyecto si no existe una compatible.",
						"parameters", java.util.Map.of("type", "object",
								"properties",
								java.util.Map.of("project_id", java.util.Map.of("type", "integer"), "name",
										java.util.Map.of("type", "string"), "color",
										java.util.Map.of("type", "string")),
								"required", java.util.List.of("project_id", "name"), "additionalProperties", false))));
	}

	private String required(String value, String message) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(message);
		return value.trim();
	}

	private Long requiredId(Long value, String message) {
		if (value == null)
			throw new IllegalArgumentException(message);
		return value;
	}

	private DocumentTypeEnum parseDocumentType(String value) {
		return "MEETING_MINUTES".equalsIgnoreCase(value) ? DocumentTypeEnum.MEETING_MINUTES : DocumentTypeEnum.DOCUMENT;
	}

	private String normalizeForMatch(String value) {
		if (value == null)
			return "";
		String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
		return normalized.toLowerCase(java.util.Locale.ROOT).trim();
	}

	private void validateProject(Long actual, Long expected) {
		if (!expected.equals(actual))
			throw new org.springframework.security.access.AccessDeniedException("El recurso pertenece a otro proyecto");
	}

	private void validateColumn(Long projectId, Long columnId) {
		if (columnId == null
				|| columns.findById(columnId).filter(column -> projectId.equals(column.getProjectId())).isEmpty())
			throw new IllegalArgumentException("La columna no pertenece al proyecto");
	}

	private void validateMember(Long projectId, Long userId) {
		if (userId != null && !members.existsByIdProjectIdAndIdUserId(projectId, userId))
			throw new org.springframework.security.access.AccessDeniedException(
					"El responsable no pertenece al proyecto");
	}

	private AiModelTierEnum selectAvailableTier(Long projectId, AiModelTierEnum preferred) {
		AiModelTierEnum chosen = preferred != null ? preferred : AiModelTierEnum.PRIMARY;
		for (AiModelTierEnum tier : AiQuotaService.modelPriority()) {
			if (chosen != null && tier != chosen && preferred != null)
				continue;
			if (projectId == null || quotaService.canUse(projectId, tier, 0L))
				return tier;
		}
		if (projectId != null) {
			for (AiModelTierEnum tier : AiQuotaService.modelPriority()) {
				if (quotaService.canUse(projectId, tier, 0L))
					return tier;
			}
		}
		return null;
	}

	private String modelLevel(AiModelTierEnum tier) {
		if (tier == AiModelTierEnum.SECONDARY)
			return "standard";
		if (tier == AiModelTierEnum.FALLBACK)
			return "basic";
		return "advanced";
	}

	private AiQuotaStatusDTO quotaStatusForProject(Long projectId, AiModelTierEnum tier) {
		if (projectId == null)
			return null;
		var quota = quotaService.getOrCreateQuota(projectId, tier);
		return quotaService.toStatus(quota);
	}

	private void recordUsage(Long projectId, Long userId, AiModelTierEnum tier, String model, long tokensConsumed) {
		if (projectId == null)
			return;
		AiUsageLogEntity usageLog = new AiUsageLogEntity();
		usageLog.setProjectId(projectId);
		usageLog.setUserId(userId);
		usageLog.setModelTier(tier);
		usageLog.setModelName(model == null ? tier.name() : model);
		usageLog.setRequestTokens(0L);
		usageLog.setResponseTokens(tokensConsumed);
		usageLog.setTotalTokens(tokensConsumed);
		usageLog.setResult("SUCCESS");
		usageLog.setFallbackUsed(false);
		usageLogs.save(usageLog);
		quotaService.consume(projectId, tier, tokensConsumed);
	}

	private String extractAiServiceError(String payload) {
		if (payload == null || payload.isBlank())
			return null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(payload);
			if (node == null)
				return null;
			if (node.has("status_message"))
				return node.get("status_message").asText();
			if (node.has("message"))
				return node.get("message").asText();
			if (node.has("detail"))
				return node.get("detail").asText();
			if (node.has("error") && node.get("error").isObject()) {
				JsonNode error = node.get("error");
				if (error.has("message"))
					return error.get("message").asText();
			}
		} catch (Exception ignored) {
			log.debug("No se pudo parsear el detalle del error del microservicio IA: {}", payload);
		}
		return payload.length() > 220 ? payload.substring(0, 220) + "..." : payload;
	}

	private AiMessageEntity saveMessage(Long conversationId, AiRoleEnum role, String content) {
		AiMessageEntity entity = new AiMessageEntity();
		entity.setConversationId(conversationId);
		entity.setRole(role);
		entity.setContent(content);
		return messages.save(entity);
	}

	private AiProjectContextDTO buildContext(Long projectId) {
		if (projectId == null)
			return null;
		ProjectEntity project = projects.findById(projectId).orElse(null);
		if (project == null)
			return null;
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
						column.getId(), column.getName(), column.getPosition()))
				.toList());
		context.setTasks(tasks.findByProjectIdOrderByColumnIdAscPositionAsc(projectId).stream()
				.map(task -> new com.syncra.gestion_proyectos.dto.ai.AiTaskContextDTO(
						task.getId(), task.getTitle(), limit(task.getDescription(), 300), task.getColumnId(),
						task.getSprintId(), task.getDueDate(), task.getAssignedTo()))
				.toList());
		context.setSprints(sprints.findByProjectId(projectId).stream()
				.map(sprint -> new com.syncra.gestion_proyectos.dto.ai.AiSprintContextDTO(
						sprint.getId(), sprint.getName(), sprint.getStartDate(), sprint.getEndDate(),
						sprint.getStatus() == null ? null : sprint.getStatus().name()))
				.toList());
		context.setMembers(members.findByIdProjectId(projectId).stream().map(ProjectMemberEntity::getId)
				.map(id -> users.findById(id.getUserId()).map(user -> new AiMemberContextDTO(
						user.getId(), user.getFirstName() + " " + user.getLastName(),
						user.getRole() == null ? null : user.getRole().name())).orElse(null))
				.filter(java.util.Objects::nonNull).toList());
		context.setDocuments(documents.findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(projectId).stream()
				.map(document -> new AiDocumentContextDTO(document.getId(), document.getTitle(),
						document.getDocumentType() == null ? null : document.getDocumentType().name(),
						document.getParentDocumentId(),
						document.getStatus() == null ? null : document.getStatus().name(),
						limit(document.getContent(), 1500), document.getMeetingType()))
				.toList());
		return context;
	}

	private String limit(String value, int max) {
		if (value == null || value.length() <= max)
			return value;
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