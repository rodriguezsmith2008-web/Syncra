package com.syncra.gestion_proyectos.service.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.entity.kanban.KanbanColumnEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.entity.task.TaskEntity;
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
	private static final String[] TASK_COLORS = {
			"#2563eb", "#16a34a", "#f97316", "#dc2626", "#8b5cf6", "#0891b2", "#65a30d"
	};

	public List<AiMessageResponseDTO> list(Long conversationId, Long userId) {
		conversationService.owned(conversationId, userId);
		return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
				.map(this::toResponse).toList();
	}

	// =========================================================================
	// DIRECTO: Edición de documentos (sin LLM)
	// =========================================================================

	private AiMessageResponseDTO tryDirectDocumentEdit(
			AiConversationEntity conversation, Long userId, String text) {
		if (text == null || conversation == null) return null;
		Long projectId = conversation.getProjectId();
		if (projectId == null) return null;

		if (!matchesDirectEditIntent(text)) return null;

		String docName = extractDocumentName(text);
		if (docName == null || docName.isBlank()) return null;

		List<DocumentEntity> candidates =
				documents.findByProjectIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(projectId, docName);

		if (candidates.isEmpty() || candidates.size() > 1) {
			log.info("Edición directa abortada: {} coincidencias para '{}'", candidates.size(), docName);
			return null;
		}

		DocumentEntity doc = candidates.get(0);

		String contenidoNuevo = extractNewContent(text);
		if (contenidoNuevo == null || contenidoNuevo.isBlank()) {
			contenidoNuevo = "\n<p><em>[Nota agregada por Nara el "
					+ LocalDate.now() + "]</em></p>";
		}

		String contenidoOriginal = doc.getContent() == null ? "" : doc.getContent();
		String contenidoFinal = contenidoOriginal + contenidoNuevo;

		DocumentUpdateDTO update = new DocumentUpdateDTO();
		update.setContent(contenidoFinal);
		documentService.update(doc.getId(), update, userId);

		saveMessage(conversation.getId(), AiRoleEnum.USER, text.trim());

		String reply = "Documento \"" + doc.getTitle() + "\" actualizado correctamente (id=" + doc.getId() + "). "
				+ "Se añadió contenido al final sin modificar el resto.";

		AiMessageEntity assistant = saveMessage(conversation.getId(), AiRoleEnum.ASSISTANT, reply);
		AiMessageResponseDTO result = toResponse(assistant);
		result.setExecutedActions(List.of(
				"Documento actualizado: " + doc.getTitle() + " (id=" + doc.getId() + ")"));
		result.setModel("direct-edit");
		result.setLevel("advanced");
		result.setFallback(false);
		result.setStatusMessage("Edición determinista, no consumió cuota de IA.");
		result.setQuota(quotaStatusForProject(projectId, AiModelTierEnum.PRIMARY));

		log.info("Edición directa aplicada sobre documento '{}' (id={})", doc.getTitle(), doc.getId());
		return result;
	}

	private boolean matchesDirectEditIntent(String text) {
		if (text == null) return false;
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		boolean hasVerb = lower.matches(".*\\b(edita|editar|modifica|modificar|actualiza|actualizar|agrega|agregar|añade|añadir|anade|anadir)\\b.*");
		boolean hasDoc = lower.matches(".*\\b(documento|doc)\\b.*");
		return hasVerb && hasDoc;
	}

	private String extractDocumentName(String text) {
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		int idx;
		int docIdx = lower.indexOf("documento");
		if (docIdx >= 0) {
			idx = docIdx + "documento".length();
		} else {
			int shortIdx = lower.indexOf("doc ");
			if (shortIdx < 0) return null;
			idx = shortIdx + "doc ".length();
		}

		String rest = text.substring(idx).trim();
		String restLower = rest.toLowerCase(java.util.Locale.ROOT);

		if (restLower.startsWith("de ")) rest = rest.substring(3).trim();
		if (restLower.startsWith("llamado ")) rest = rest.substring(8).trim();
		if (restLower.startsWith("titulado ")) rest = rest.substring(9).trim();

		int cut = rest.length();
		String[] cutMarkers = { " y ", " agregando", " añadiendo", " anadiendo", " añade", " anade",
				" agrega", ",", ".", "\n" };
		for (String marker : cutMarkers) {
			int m = rest.toLowerCase(java.util.Locale.ROOT).indexOf(marker.toLowerCase(java.util.Locale.ROOT));
			if (m >= 0 && m < cut) cut = m;
		}

		String name = rest.substring(0, cut).trim();
		return name.isBlank() ? null : name;
	}

	private String extractNewContent(String text) {
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("[\"'\u201C\u201D\u2018\u2019]([^\"'\u201C\u201D\u2018\u2019]{3,})[\"'\u201C\u201D\u2018\u2019]")
				.matcher(text);
		if (m.find()) {
			return "\n<p>" + m.group(1).trim() + "</p>";
		}
		return null;
	}

	// =========================================================================
	// DIRECTO: Pasar compromisos de acta al tablero (sin LLM)
	// =========================================================================

	private AiMessageResponseDTO tryDirectCommitmentsToBoard(
			AiConversationEntity conversation, Long userId, String text) {
		if (text == null || conversation == null) return null;
		Long projectId = conversation.getProjectId();
		if (projectId == null) return null;
		if (!matchesCommitmentsIntent(text)) return null;

		String actaName = extractActaName(text);
		List<DocumentEntity> actas;
		if (actaName != null && !actaName.isBlank()) {
			actas = documents.findByProjectIdAndDocumentTypeAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
					projectId, DocumentTypeEnum.MEETING_MINUTES, actaName);
		} else {
			actas = documents.findByProjectIdAndDocumentTypeAndDeletedAtIsNull(
					projectId, DocumentTypeEnum.MEETING_MINUTES);
		}
		if (actas.isEmpty()) {
			log.info("Acta→tareas abortado: 0 actas encontradas (filtro='{}')", actaName);
			return null;
		}

		sortByUpdatedDesc(actas);
		DocumentEntity acta = actas.get(0);

		List<CommitmentRow> rows = parseCommitmentsFromHtml(acta.getContent());
		if (rows.isEmpty()) {
			log.info("Acta→tareas abortado: 0 filas de compromisos en acta '{}' (id={})",
					acta.getTitle(), acta.getId());
			return null;
		}

		List<KanbanColumnEntity> cols = columns.findByProjectIdOrderByPositionAsc(projectId);
		if (cols.isEmpty()) {
			log.info("Acta→tareas abortado: el proyecto no tiene columnas");
			return null;
		}

		List<String> executed = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		int createdCount = 0;

		for (CommitmentRow row : rows) {
			try {
				Long columnId = matchColumnForStatus(row.estado, cols);
				if (columnId == null) {
					warnings.add("No hay columna compatible para estado '" + row.estado
							+ "' en la tarea '" + row.actividad + "'");
					continue;
				}
				Long assignedTo = matchMemberForName(row.responsable, projectId);
				if (row.responsable != null && !row.responsable.isBlank() && assignedTo == null) {
					warnings.add("Sin responsable identificado para '" + row.actividad
							+ "' (acta dice: '" + row.responsable + "')");
				}

				TaskRequestDTO dto = new TaskRequestDTO();
				dto.setTitle(row.actividad.length() > 300 ? row.actividad.substring(0, 300) : row.actividad);
				dto.setDescription("Creado desde acta: \"" + acta.getTitle()
						+ "\" (id=" + acta.getId() + ")"
						+ (row.estado != null && !row.estado.isBlank() ? "\nEstado en el acta: " + row.estado : ""));
				dto.setColumnId(columnId);
				dto.setAssignedTo(assignedTo);
				if (row.fechaEntrega != null && !row.fechaEntrega.isBlank()) {
					try {
						dto.setDueDate(LocalDate.parse(row.fechaEntrega.trim()));
					} catch (Exception e) {
						warnings.add("Fecha inválida '" + row.fechaEntrega + "' en '" + row.actividad + "'");
					}
				}
				dto.setColor(TASK_COLORS[(int) (Math.random() * TASK_COLORS.length)]);

				TaskResponseDTO created = taskService.create(projectId, userId, dto);
				executed.add("Tarea creada: " + created.getTitle() + " (id=" + created.getId() + ")");
				createdCount++;
			} catch (Exception ex) {
				warnings.add("Error al crear '" + row.actividad + "': " + ex.getMessage());
				log.warn("Fallo creando tarea desde acta: {}", ex.getMessage());
			}
		}

		if (createdCount == 0 && warnings.isEmpty()) {
			log.info("Acta→tareas abortado: no se creó ninguna tarea ni hubo warnings");
			return null;
		}

		saveMessage(conversation.getId(), AiRoleEnum.USER, text.trim());

		StringBuilder reply = new StringBuilder();
		reply.append("Procesé el acta \"").append(acta.getTitle())
				.append("\" (id=").append(acta.getId()).append("). ");
		reply.append("Creé ").append(createdCount).append(" tarea(s) en el tablero.");
		if (!warnings.isEmpty()) {
			reply.append("\n\nAvisos:\n- ").append(String.join("\n- ", warnings));
		}

		AiMessageEntity assistant = saveMessage(conversation.getId(), AiRoleEnum.ASSISTANT, reply.toString());
		AiMessageResponseDTO result = toResponse(assistant);
		result.setExecutedActions(executed);
		result.setModel("direct-commitments");
		result.setLevel("advanced");
		result.setFallback(false);
		result.setStatusMessage("Procesamiento determinista, no consumió cuota de IA.");
		result.setQuota(quotaStatusForProject(projectId, AiModelTierEnum.PRIMARY));

		log.info("Acta→tareas aplicado: acta '{}' (id={}) → {} tareas creadas, {} avisos",
				acta.getTitle(), acta.getId(), createdCount, warnings.size());
		return result;
	}

	private boolean matchesCommitmentsIntent(String text) {
		if (text == null) return false;
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		boolean hasActa = lower.matches(".*\\bactas?\\b.*");
		if (!hasActa) return false;
		boolean hasTarget = lower.matches(".*\\b(tablero|board|tareas?|compromisos?)\\b.*");
		boolean hasTransferVerb = lower.matches(".*\\b(pasa|pasar|pásal[ao]s?|pasal[ao]s?|envia|enviar|envíal[ao]s?|envial[ao]s?|crea|crear|lleva|llevar|convierte|convertir|mueve|mover|procesa|procesar)\\b.*");
		return hasTarget && hasTransferVerb;
	}

	private String extractActaName(String text) {
		if (text == null) return null;
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		int idx = lower.indexOf("acta");
		if (idx < 0) return null;
		String rest = text.substring(idx + "acta".length()).trim();
		String restLower = rest.toLowerCase(java.util.Locale.ROOT);

		if (restLower.startsWith("de la ")) rest = rest.substring(6).trim();
		else if (restLower.startsWith("del ")) rest = rest.substring(4).trim();
		else if (restLower.startsWith("de ")) rest = rest.substring(3).trim();

		int cut = rest.length();
		String[] cutMarkers = { " al tablero", " a el tablero", " al board", " como tareas",
				" al board ", " a tareas", " y ", ",", ".", "\n" };
		for (String marker : cutMarkers) {
			int m = rest.toLowerCase(java.util.Locale.ROOT).indexOf(marker.toLowerCase(java.util.Locale.ROOT));
			if (m >= 0 && m < cut) cut = m;
		}
		String name = rest.substring(0, cut).trim();
		String nameLower = name.toLowerCase(java.util.Locale.ROOT);
		if (name.isBlank()
				|| nameLower.startsWith("al ")
				|| nameLower.startsWith("a el ")
				|| nameLower.startsWith("como ")
				|| nameLower.equals("al")
				|| nameLower.equals("y")) {
			return null;
		}
		return name;
	}

	private List<CommitmentRow> parseCommitmentsFromHtml(String html) {
		List<CommitmentRow> result = new ArrayList<>();
		if (html == null || html.isBlank()) return result;
		try {
			org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
			org.jsoup.nodes.Element heading = null;
			for (org.jsoup.nodes.Element h : doc.select("h1, h2, h3, h4, h5")) {
				String t = h.text().toLowerCase(java.util.Locale.ROOT).trim();
				if (t.contains("compromiso")) { heading = h; break; }
			}
			if (heading == null) return result;

			org.jsoup.nodes.Element current = heading;
			org.jsoup.nodes.Element table = null;
			while (current != null) {
				current = current.nextElementSibling();
				if (current == null) break;
				if ("table".equalsIgnoreCase(current.tagName())) { table = current; break; }
			}
			if (table == null) return result;

			for (org.jsoup.nodes.Element tr : table.select("tr")) {
				org.jsoup.select.Elements cells = tr.select("th, td");
				if (cells.size() < 4) continue;

				String actividad = cells.get(0).text().trim();
				if (actividad.isBlank()) continue;
				String actLower = actividad.toLowerCase(java.util.Locale.ROOT);
				if (actLower.equals("actividad") || actLower.equals("tarea") || actLower.equals("compromiso")) continue;

				CommitmentRow row = new CommitmentRow();
				row.actividad = actividad;
				row.responsable = cells.get(1).text().trim();
				row.fechaEntrega = cells.get(2).text().trim();
				row.estado = cells.get(3).text().trim();
				result.add(row);
			}
		} catch (Exception ex) {
			log.warn("Error parseando compromisos del acta: {}", ex.getMessage());
		}
		return result;
	}

	private Long matchColumnForStatus(String estado, List<KanbanColumnEntity> cols) {
		if (cols == null || cols.isEmpty()) return null;
		String needle = normalizeForMatch(estado);
		if (needle.isBlank()) return cols.get(0).getId();

		String[] keywords;
		if (needle.contains("asignad") || needle.contains("nuev") || needle.contains("pendient")
				|| needle.contains("backlog") || needle.contains("todo")) {
			keywords = new String[]{"asignad", "nuev", "pendient", "backlog", "todo", "tarea"};
		} else if (needle.contains("proceso") || needle.contains("progres") || needle.contains("curso")
				|| needle.contains("doing") || needle.contains("wip")) {
			keywords = new String[]{"proceso", "progres", "curso", "doing", "wip"};
		} else if (needle.contains("termin") || needle.contains("complet") || needle.contains("finaliz")
				|| needle.contains("hech") || needle.contains("done") || needle.contains("list")) {
			keywords = new String[]{"termin", "complet", "final", "hech", "done", "list"};
		} else if (needle.contains("detenid") || needle.contains("bloque") || needle.contains("paus")
				|| needle.contains("block")) {
			keywords = new String[]{"deten", "bloque", "paus", "block"};
		} else {
			keywords = new String[]{needle};
		}

		for (String keyword : keywords) {
			for (KanbanColumnEntity col : cols) {
				String colName = normalizeForMatch(col.getName());
				if (colName.contains(keyword)) return col.getId();
			}
		}
		return null;
	}

	private Long matchMemberForName(String nombre, Long projectId) {
		if (nombre == null || nombre.isBlank()) return null;
		String needle = normalizeForMatch(nombre);
		if (needle.isBlank()) return null;

		if (needle.contains("equipo")
				|| needle.contains("todos")
				|| needle.contains("completo")
				|| needle.contains("varios")
				|| needle.contains("ninguno")
				|| needle.contains("sin asignar")) {
			return null;
		}

		String[] tokens = needle.split("\\s+");
		List<Long> fullMatches = new ArrayList<>();
		List<Long> partialMatches = new ArrayList<>();

		for (ProjectMemberEntity m : members.findByIdProjectId(projectId)) {
			Long uid = m.getId().getUserId();
			var user = users.findById(uid).orElse(null);
			if (user == null) continue;

			String fullName = normalizeForMatch(
					(user.getFirstName() == null ? "" : user.getFirstName()) + " "
							+ (user.getLastName() == null ? "" : user.getLastName()));

			boolean allMatch = true;
			for (String t : tokens) {
				if (!fullName.contains(t)) { allMatch = false; break; }
			}
			if (allMatch) fullMatches.add(uid);

			for (String t : tokens) {
				if (t.length() >= 3 && fullName.contains(t)) {
					if (!partialMatches.contains(uid)) partialMatches.add(uid);
					break;
				}
			}
		}

		if (fullMatches.size() == 1) return fullMatches.get(0);
		if (partialMatches.size() == 1) return partialMatches.get(0);
		return null;
	}

	// =========================================================================
	// DIRECTO: Crear acta de seguimiento o entrega (sin LLM)
	// =========================================================================

	private AiMessageResponseDTO tryDirectCreateFollowUpActa(
			AiConversationEntity conversation, Long userId, String text) {
		if (text == null || conversation == null) return null;
		Long projectId = conversation.getProjectId();
		if (projectId == null) return null;
		if (!matchesCreateActaIntent(text)) return null;

		String kind = extractActaKind(text); // "SEGUIMIENTO" o "ENTREGA"
		if (kind == null) return null;

		List<DocumentEntity> actas = documents.findByProjectIdAndDocumentTypeAndDeletedAtIsNull(
				projectId, DocumentTypeEnum.MEETING_MINUTES);

		if (actas.isEmpty()) {
			return buildDirectActaError(conversation, userId, text,
					"No hay actas previas en el proyecto. Primero crea un acta de planeación.");
		}

		String ruleError = validateActaCreationRules(actas, kind);
		if (ruleError != null) {
			return buildDirectActaError(conversation, userId, text, ruleError);
		}

		sortByCreatedDesc(actas);
		log.info("Actas disponibles (por createdAt desc): {}", actas.stream()
			.map(a -> a.getId() + ":'" + a.getTitle() + "':" + a.getMeetingType())
			.limit(10).toList());
		DocumentEntity base = findBaseActa(actas, extractBaseActaName(text));
		if (base == null) base = actas.get(0);

		// Tareas del tablero creadas desde la acta base
		String tagPrefix = "Creado desde acta: \"" + base.getTitle() + "\"";
		List<TaskEntity> baseTasks = new ArrayList<>();
		for (TaskEntity t : tasks.findByProjectIdOrderByColumnIdAscPositionAsc(projectId)) {
			if (t.getDescription() != null && t.getDescription().contains(tagPrefix)) {
				baseTasks.add(t);
			}
		}

		if (baseTasks.isEmpty()) {
			return buildDirectActaError(conversation, userId, text,
					"Elegí como acta base: \"" + base.getTitle() + "\" (id=" + base.getId()
					+ ", tipo=" + (base.getMeetingType() == null ? "desconocido" : base.getMeetingType()) + ").\n\n"
					+ "Esa acta no tiene tareas asociadas en el tablero. Antes de crear el seguimiento, "
					+ "necesito que esas tareas existan.\n\n"
					+ "Hazlo así:\n"
					+ "1. \"pasa las tareas del acta " + base.getTitle() + " al tablero\"\n"
					+ "2. Luego: \"crea un acta de seguimiento basada en el acta " + base.getTitle() + "\"\n\n"
					+ "Si querías usar otra acta como base, dime su nombre exacto.");
		}

		List<KanbanColumnEntity> cols = columns.findByProjectIdOrderByPositionAsc(projectId);
		LocalDate today = LocalDate.now();

		List<CommitmentRow> newRows = new ArrayList<>();
		List<String> moved = new ArrayList<>();
		List<String> warnings = new ArrayList<>();

		for (TaskEntity t : baseTasks) {
			String nuevoEstado = computeNewState(t, cols, today);
			Long targetColumnId = matchColumnForStatus(nuevoEstado, cols);

			if (targetColumnId != null && !targetColumnId.equals(t.getColumnId())) {
				try {
					TaskMoveDTO moveDto = new TaskMoveDTO();
					moveDto.setColumnId(targetColumnId);
					moveDto.setPosition(0L);
					taskService.move(t.getId(), userId, moveDto);
					moved.add(t.getTitle());
				} catch (Exception e) {
					warnings.add("No se pudo mover '" + t.getTitle() + "': " + e.getMessage());
				}
			}

			String responsableNombre = "";
			if (t.getAssignedTo() != null) {
				var u = users.findById(t.getAssignedTo()).orElse(null);
				if (u != null) responsableNombre = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
						+ (u.getLastName() == null ? "" : u.getLastName())).trim();
			}

			CommitmentRow row = new CommitmentRow();
			row.actividad = t.getTitle();
			row.responsable = responsableNombre;
			row.fechaEntrega = t.getDueDate() == null ? "" : t.getDueDate().toString();
			row.estado = nuevoEstado;
			newRows.add(row);
		}

				// Título único
		String baseTitle = kind.equals("SEGUIMIENTO") ? "Acta de Seguimiento" : "Acta de Entrega";
		String title = baseTitle + " - " + today;
		int suffix = 2;
		while (true) {
			final String candidate = title;
			boolean existe = documents.findByProjectIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(projectId, candidate)
					.stream().anyMatch(d -> d.getTitle().equalsIgnoreCase(candidate));
			if (!existe) break;
			title = baseTitle + " - " + today + " (" + suffix + ")";
			suffix++;
		}

		// Crear el documento
		DocumentRequestDTO req = new DocumentRequestDTO();
		req.setTitle(title);
		req.setDocumentType(DocumentTypeEnum.MEETING_MINUTES);
		req.setMeetingType(kind);
		req.setSprintId(base.getSprintId());
		req.setQuarter(base.getQuarter());

		var created = documentService.create(projectId, req, userId);

		// Construir contenido y actualizar
		String newContent = buildActaHtmlForFollowUp(base, kind, today, newRows);
		if (newContent != null && !newContent.isBlank()) {
			DocumentUpdateDTO upd = new DocumentUpdateDTO();
			upd.setContent(newContent);
			documentService.update(created.getId(), upd, userId);
		}

		saveMessage(conversation.getId(), AiRoleEnum.USER, text.trim());

		StringBuilder reply = new StringBuilder();
		reply.append("Acta de ").append(kind.equals("SEGUIMIENTO") ? "Seguimiento" : "Entrega")
				.append(" creada: \"").append(created.getTitle())
				.append("\" (id=").append(created.getId()).append(").");
		reply.append("\nBasada en: \"").append(base.getTitle())
				.append("\" (id=").append(base.getId()).append(").");
		reply.append("\nCompromisos procesados: ").append(newRows.size()).append(".");
		if (!moved.isEmpty()) {
			reply.append("\nTareas movidas en el tablero: ").append(moved.size()).append(".");
		}
		if (!warnings.isEmpty()) {
			reply.append("\n\nAvisos:\n- ").append(String.join("\n- ", warnings));
		}

		AiMessageEntity assistant = saveMessage(conversation.getId(), AiRoleEnum.ASSISTANT, reply.toString());
		AiMessageResponseDTO result = toResponse(assistant);
		List<String> executed = new ArrayList<>();
		executed.add("Documento creado: " + created.getTitle() + " (id=" + created.getId() + ")");
		for (String m : moved) executed.add("Tarea movida: " + m);
		result.setExecutedActions(executed);
		result.setModel("direct-acta");
		result.setLevel("advanced");
		result.setFallback(false);
		result.setStatusMessage("Creación de acta determinista, no consumió cuota de IA.");
		result.setQuota(quotaStatusForProject(projectId, AiModelTierEnum.PRIMARY));

		log.info("Acta→seguimiento/entrega creada: '{}' (id={}) desde '{}' (id={}), {} tareas, {} movidas",
				created.getTitle(), created.getId(), base.getTitle(), base.getId(), newRows.size(), moved.size());
		return result;
	}

	private AiMessageResponseDTO buildDirectActaError(
			AiConversationEntity conversation, Long userId, String text, String message) {
		saveMessage(conversation.getId(), AiRoleEnum.USER, text.trim());
		AiMessageEntity assistant = saveMessage(conversation.getId(), AiRoleEnum.ASSISTANT, message);
		AiMessageResponseDTO result = toResponse(assistant);
		result.setExecutedActions(List.of());
		result.setModel("direct-acta");
		result.setLevel("advanced");
		result.setFallback(false);
		result.setStatusMessage(message);
		result.setQuota(quotaStatusForProject(conversation.getProjectId(), AiModelTierEnum.PRIMARY));
		return result;
	}

	private boolean matchesCreateActaIntent(String text) {
		if (text == null) return false;
		String lower = normalizeForMatch(text);
		boolean hasVerb = lower.matches(".*\\b(crea|crear|genera|generar|haz|hacer|nueva|nuevo|agrega|agregar)\\b.*");
		boolean hasType = lower.contains("seguimiento") || lower.contains("entrega");
		boolean hasActa = lower.matches(".*\\bactas?\\b.*");
		return hasVerb && hasType && hasActa;
	}

	private String extractActaKind(String text) {
		String lower = normalizeForMatch(text);
		// Detectar "seguimiento" no como parte del nombre del acta base
		int idxSeg = lower.indexOf("seguimiento");
		int idxEnt = lower.indexOf("entrega");
		if (idxSeg < 0 && idxEnt < 0) return null;
		if (idxSeg >= 0 && idxEnt < 0) return "SEGUIMIENTO";
		if (idxEnt >= 0 && idxSeg < 0) return "ENTREGA";
		return idxSeg < idxEnt ? "SEGUIMIENTO" : "ENTREGA";
	}

	private String extractBaseActaName(String text) {
		if (text == null) return null;
		String lower = text.toLowerCase(java.util.Locale.ROOT);

		// Buscar la SEGUNDA ocurrencia de "acta" (la primera suele ser "acta de seguimiento")
		int first = lower.indexOf("acta");
		if (first < 0) return null;
		int second = lower.indexOf("acta", first + 4);
		if (second < 0) return null;

		String rest = text.substring(second + 4).trim();
		String restLower = rest.toLowerCase(java.util.Locale.ROOT);
		if (restLower.startsWith("de la ")) rest = rest.substring(6).trim();
		else if (restLower.startsWith("del ")) rest = rest.substring(4).trim();
		else if (restLower.startsWith("de ")) rest = rest.substring(3).trim();

		int cut = rest.length();
		String[] cutMarkers = { " y ", ",", ".", "\n", " para ", " con fecha " };
		for (String c : cutMarkers) {
			int m = rest.toLowerCase(java.util.Locale.ROOT).indexOf(c.toLowerCase(java.util.Locale.ROOT));
			if (m >= 0 && m < cut) cut = m;
		}
		String name = rest.substring(0, cut).trim();
		if (name.isBlank() || name.length() < 3) return null;
		return name;
	}

	private DocumentEntity findBaseActa(List<DocumentEntity> actas, String baseName) {
		if (baseName == null || baseName.isBlank()) return null;
		String baseNorm = normalizeForMatch(baseName);

		// ¿Es un tipo ("planeacion", "seguimiento", "entrega")?
		if (baseNorm.contains("planeacion") || baseNorm.contains("planeación")) {
			for (DocumentEntity a : actas) {
				if ("PLANEACION".equalsIgnoreCase(a.getMeetingType())) return a;
			}
		}
		if (baseNorm.contains("seguimiento")) {
			for (DocumentEntity a : actas) {
				if ("SEGUIMIENTO".equalsIgnoreCase(a.getMeetingType())) return a;
			}
		}
		if (baseNorm.contains("entrega")) {
			for (DocumentEntity a : actas) {
				if ("ENTREGA".equalsIgnoreCase(a.getMeetingType())) return a;
			}
		}

		// Es un nombre específico
		for (DocumentEntity a : actas) {
			if (a.getTitle() != null && normalizeForMatch(a.getTitle()).contains(baseNorm)) return a;
		}
		return null;
	}

	private String validateActaCreationRules(List<DocumentEntity> actas, String kind) {
		if (actas.isEmpty()) {
			return "No hay actas previas en el proyecto.";
		}
		List<DocumentEntity> sorted = new ArrayList<>(actas);
		sortByCreatedDesc(sorted);
		DocumentEntity last = sorted.get(0);
		String lastKind = last.getMeetingType() == null ? "" : last.getMeetingType().toUpperCase(java.util.Locale.ROOT);

		boolean hasPlaneacion = actas.stream()
				.anyMatch(a -> "PLANEACION".equalsIgnoreCase(a.getMeetingType()));
		boolean hasSeguimiento = actas.stream()
				.anyMatch(a -> "SEGUIMIENTO".equalsIgnoreCase(a.getMeetingType()));

		if ("SEGUIMIENTO".equals(kind)) {
			if (!"PLANEACION".equals(lastKind) && !"SEGUIMIENTO".equals(lastKind)) {
				return "No se puede crear un acta de seguimiento porque la última acta es de tipo '"
						+ (lastKind.isBlank() ? "desconocido" : lastKind)
						+ "'. Solo se puede continuar desde una planeación o un seguimiento.";
			}
			return null;
		}
		if ("ENTREGA".equals(kind)) {
			if (!hasPlaneacion || !hasSeguimiento) {
				return "No se puede crear un acta de entrega sin al menos una planeación y un seguimiento previos.";
			}
			return null;
		}
		return "Tipo de acta no reconocido.";
	}

	private String computeNewState(TaskEntity t, List<KanbanColumnEntity> cols, LocalDate today) {
		// 1. Fecha pasada → Terminado
		if (t.getDueDate() != null && t.getDueDate().isBefore(today)) {
			return "Terminado";
		}
		// 2. Según columna actual
		KanbanColumnEntity col = null;
		for (KanbanColumnEntity c : cols) {
			if (c.getId().equals(t.getColumnId())) { col = c; break; }
		}
		if (col == null) return "Asignado";
		String name = normalizeForMatch(col.getName());
		if (name.contains("progres") || name.contains("proceso") || name.contains("curso")
				|| name.contains("doing") || name.contains("wip")) {
			return "En Proceso";
		}
		if (name.contains("deten") || name.contains("bloque") || name.contains("paus") || name.contains("block")) {
			return "Detenido";
		}
		if (name.contains("termin") || name.contains("complet") || name.contains("final") || name.contains("done")) {
			return "Terminado";
		}
		return "Asignado";
	}

	private String buildActaHtmlForFollowUp(DocumentEntity base, String kind, LocalDate today,
			List<CommitmentRow> rows) {
		String baseHtml = base.getContent();
		if (baseHtml == null || baseHtml.isBlank()) {
			return buildMinimalActaHtml(kind, today, rows);
		}
		try {
			org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(baseHtml);

			// Actualizar fecha en la primera tabla
			for (org.jsoup.nodes.Element th : doc.select("th")) {
				String t = th.text().trim().toLowerCase(java.util.Locale.ROOT);
				if (t.equals("fecha")) {
					org.jsoup.nodes.Element td = th.nextElementSibling();
					if (td != null && "td".equals(td.tagName())) {
						td.html("");
						td.appendElement("p").text(today.toString());
					}
				}
			}
			// Actualizar motivo
			for (org.jsoup.nodes.Element th : doc.select("th")) {
				String t = th.text().trim().toLowerCase(java.util.Locale.ROOT);
				if (t.contains("motivo")) {
					org.jsoup.nodes.Element td = th.nextElementSibling();
					if (td != null && "td".equals(td.tagName())) {
						td.html("");
						td.appendElement("p").text(kind.equals("SEGUIMIENTO")
								? "Seguimiento del avance del proyecto"
								: "Cierre y entrega del proyecto");
					}
				}
			}

			// Reemplazar tabla de Compromisos
			org.jsoup.nodes.Element heading = null;
			for (org.jsoup.nodes.Element h : doc.select("h1, h2, h3, h4, h5")) {
				if (h.text().toLowerCase(java.util.Locale.ROOT).contains("compromiso")) {
					heading = h; break;
				}
			}
			if (heading != null) {
				org.jsoup.nodes.Element current = heading;
				org.jsoup.nodes.Element table = null;
				while (current != null) {
					current = current.nextElementSibling();
					if (current == null) break;
					if ("table".equalsIgnoreCase(current.tagName())) { table = current; break; }
				}
				if (table != null) {
					org.jsoup.nodes.Element tbody = table.selectFirst("tbody");
					if (tbody == null) tbody = table;
					tbody.html("");

					org.jsoup.nodes.Element headerRow = tbody.appendElement("tr");
					headerRow.appendElement("th").appendElement("p").text("Actividad");
					headerRow.appendElement("th").appendElement("p").text("Responsable");
					headerRow.appendElement("th").appendElement("p").text("Fecha Entrega");
					headerRow.appendElement("th").appendElement("p")
							.text("Estado (Asignado/En Proceso/Terminado/Detenido)");

					for (CommitmentRow r : rows) {
						org.jsoup.nodes.Element tr = tbody.appendElement("tr");
						tr.appendElement("td").appendElement("p").text(r.actividad == null ? "" : r.actividad);
						tr.appendElement("td").appendElement("p").text(r.responsable == null ? "" : r.responsable);
						tr.appendElement("td").appendElement("p").text(r.fechaEntrega == null ? "" : r.fechaEntrega);
						tr.appendElement("td").appendElement("p").text(r.estado == null ? "" : r.estado);
					}
				}
			}

			doc.outputSettings().prettyPrint(false);
			return doc.body().html();
		} catch (Exception e) {
			log.warn("Error construyendo HTML del acta derivada: {}", e.getMessage());
			return buildMinimalActaHtml(kind, today, rows);
		}
	}

	private String buildMinimalActaHtml(String kind, LocalDate today, List<CommitmentRow> rows) {
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>PT-AR-01. Formato Acta de Reunion</h1>");
		sb.append("<p><strong>Fecha:</strong> ").append(today).append("</p>");
		sb.append("<p><strong>Tipo:</strong> ")
				.append(kind.equals("SEGUIMIENTO") ? "Seguimiento" : "Entrega").append("</p>");
		sb.append("<h2>Compromisos</h2>");
		sb.append("<table><tr>")
				.append("<th><p>Actividad</p></th>")
				.append("<th><p>Responsable</p></th>")
				.append("<th><p>Fecha Entrega</p></th>")
				.append("<th><p>Estado (Asignado/En Proceso/Terminado/Detenido)</p></th>")
				.append("</tr>");
		for (CommitmentRow r : rows) {
			sb.append("<tr>")
					.append("<td><p>").append(r.actividad == null ? "" : r.actividad).append("</p></td>")
					.append("<td><p>").append(r.responsable == null ? "" : r.responsable).append("</p></td>")
					.append("<td><p>").append(r.fechaEntrega == null ? "" : r.fechaEntrega).append("</p></td>")
					.append("<td><p>").append(r.estado == null ? "" : r.estado).append("</p></td>")
					.append("</tr>");
		}
		sb.append("</table>");
		return sb.toString();
	}

	private void sortByUpdatedDesc(List<DocumentEntity> actas) {
		actas.sort((a, b) -> {
			LocalDateTime ua = a.getUpdatedAt();
			LocalDateTime ub = b.getUpdatedAt();
			if (ua == null && ub == null) return 0;
			if (ua == null) return 1;
			if (ub == null) return -1;
			return ub.compareTo(ua);
		});
	}

	private void sortByCreatedDesc(List<DocumentEntity> actas) {
		actas.sort((a, b) -> {
			LocalDateTime ca = a.getCreatedAt();
			LocalDateTime cb = b.getCreatedAt();
			if (ca == null && cb == null) return 0;
			if (ca == null) return 1;
			if (cb == null) return -1;
			return cb.compareTo(ca);
		});
	}

	private static class CommitmentRow {
		String actividad;
		String responsable;
		String fechaEntrega;
		String estado;
	}

	// =========================================================================
	// send() — orquesta todo
	// =========================================================================

	@Transactional
	public AiMessageResponseDTO send(Long conversationId, Long userId, String text) {
		AiConversationEntity conversation = conversationService.owned(conversationId, userId);
		conversationService.validateProjectAccess(userId, conversation.getProjectId());

		AiMessageResponseDTO directEditResult = tryDirectDocumentEdit(conversation, userId, text);
		if (directEditResult != null) return directEditResult;

		AiMessageResponseDTO directActaResult = tryDirectCreateFollowUpActa(conversation, userId, text);
		if (directActaResult != null) return directActaResult;

		AiMessageResponseDTO directCommitmentsResult = tryDirectCommitmentsToBoard(conversation, userId, text);
		if (directCommitmentsResult != null) return directCommitmentsResult;

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
		request.setTools(trivial ? List.of() : buildToolDefinitions());
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

		long tokensAccumulated = extractTotalTokens(response.getUsage());

		boolean emittedTools = response.getToolCalls() != null && !response.getToolCalls().isEmpty();
		if (!trivial && !emittedTools && looksLikePlan(response.getReply())) {
			log.info("Detectado plan sin ejecución. Reenviando nudge para forzar ejecución.");
			AiServiceRequestDTO nudge = new AiServiceRequestDTO();
			nudge.setMessage(
					"REINTENTO OBLIGATORIO — tu respuesta anterior fue rechazada porque solo describiste un plan sin ejecutarlo. " +
					"EJECUTA AHORA las herramientas necesarias para completar esta petición: \"" + userText + "\".\n\n" +
					"REGLA ABSOLUTA: en este turno SOLO puedes emitir tool_calls. Está prohibido emitir texto sin herramientas. " +
					"No digas 'necesito', 'voy a', 'debo', 'primero'. No expliques. No preguntes. No pidas confirmación. " +
					"Si necesitas leer un documento antes de editarlo, llama get_document y update_document en la MISMA respuesta, encadenados. " +
					"Empieza ya llamando a la primera herramienta.");
			nudge.setHistory(previous.stream().skip(Math.max(0, previous.size() - HISTORY_WINDOW))
					.map(m -> new AiHistoryItemDTO(m.getRole().name(), m.getContent())).toList());
			nudge.setContext(buildContext(conversation.getProjectId()));
			nudge.setModel(request.getModel());
			nudge.setTools(buildToolDefinitions());
			if (nudge.getContext() != null) {
				nudge.getContext().setCurrentUserId(userId);
				users.findById(userId).ifPresent(user ->
						nudge.getContext().setCurrentUserName(user.getFirstName() + " " + user.getLastName()));
			}

			try {
				AiChatResponseDTO nudged = callAiService(nudge);
				if (nudged != null) {
					tokensAccumulated += extractTotalTokens(nudged.getUsage());
					response = nudged;
				}
			} catch (Exception ex) {
				log.warn("El nudge de forzar ejecución falló: {}", ex.getMessage());
			}
		}

		List<String> executedActions = new ArrayList<>();
		int toolCallRounds = 0;

		while (response.getToolCalls() != null && !response.getToolCalls().isEmpty() && toolCallRounds < 5) {
			toolCallRounds++;
			List<String> toolExecution = executeToolCalls(response.getToolCalls(), conversation.getProjectId(), userId);
			executedActions.addAll(toolExecution);
			if (toolExecution.isEmpty())
				break;

			long remaining = quotaService.remainingTokens(conversation.getProjectId(), selectedTier);
			long minimumForNextRound = 3000L;
			if (remaining < minimumForNextRound) {
				log.info("Cuota de {} casi agotada ({} tokens restantes). Deteniendo tool calls tras {} round(s).",
						selectedTier, remaining, toolCallRounds);
				response.setReply("Ejecuté " + executedActions.size() + " acción(es) con el modelo actual:\n- "
						+ String.join("\n- ", executedActions)
						+ "\n\nLa cuota de este modelo se agotó a mitad del proceso. Manda otro mensaje para continuar con el siguiente modelo disponible.");
				response.setToolCalls(List.of());
				break;
			}

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
			followUp.setContext(null);
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
				"resume", "analiza", "miembro", "columna", "tablero", "sprint", "board",
				"edita", "editar", "modifica", "modificar", "actualiza", "actualizar"
		};
		for (String keyword : keywords) {
			if (normalized.contains(keyword))
				return false;
		}
		return true;
	}

	private boolean looksLikePlan(String text) {
		if (text == null || text.isBlank()) return false;
		String lower = text.toLowerCase(java.util.Locale.ROOT).trim();

		String[] prepVerbs = {
				"necesito ", "voy a ", "debo ", "debería ", "deberia ",
				"primero ", "ahora voy", "ahora consultaré", "ahora revisaré",
				"déjame ", "dejame ", "permíteme ", "permiteme ",
				"procedo a ", "procederé ", "procedere ", "empezaré ",
				"empezare ", "quiero ", "voy a revisar", "voy a consultar",
				"voy a leer", "voy a verificar", "voy a buscar",
				"necesito leer", "necesito consultar", "necesito revisar",
				"necesito verificar", "necesito ver ", "necesito acceder",
				"necesito abrir", "necesito obtener", "necesito buscar",
				"tengo que leer", "tengo que consultar", "tengo que revisar"
		};
		for (String verb : prepVerbs) {
			if (lower.startsWith(verb)
					|| lower.contains(". " + verb)
					|| lower.contains("\n" + verb)
					|| lower.contains("¡" + verb)
					|| lower.contains("¿" + verb)) {
				return true;
			}
		}

		String[] blockers = {
				"si me confirmas", "confírmame", "confírmame el id", "me falta el id",
				"no tengo disponible", "no tengo acceso", "no puedo acceder",
				"no tengo herramientas", "no tengo disponible las herramientas",
				"necesito que me confirmes", "necesito saber el id"
		};
		for (String b : blockers) {
			if (lower.contains(b)) return true;
		}

		return false;
	}

	private AiChatResponseDTO callAiService(AiServiceRequestDTO body) {
		try {
			return RestClient.builder().baseUrl(aiServiceUrl.trim()).build().post().uri("/ai/chat")
					.header("X-Internal-Api-Key", internalApiKey)
					.body(body).retrieve().body(AiChatResponseDTO.class);
		} catch (org.springframework.web.client.RestClientResponseException ex) {
			String aiMessage = extractAiServiceError(ex.getResponseBodyAsString());
			org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus
					.resolve(ex.getStatusCode().value());
			if (status == null) status = org.springframework.http.HttpStatus.BAD_GATEWAY;
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
		List<String> executed = new ArrayList<>();

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
						List<String> lines = new ArrayList<>();
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
								: LocalDate.parse(String.valueOf(args.get("due_date"))));
						dto.setColor(args.get("color") == null ? null : String.valueOf(args.get("color")));
						if (dto.getColumnId() != null)
							validateColumn(projectId, dto.getColumnId());
						if (dto.getAssignedTo() != null)
							validateMember(projectId, dto.getAssignedTo());
						TaskResponseDTO created = taskService.create(projectId, userId, dto);
						executed.add("Tarea creada: " + created.getTitle() + " (id=" + created.getId() + ")");
					}
					case "update_task" -> {
						Long taskId = coalesceLong(args.get("task_id"), null);
						if (taskId == null) throw new IllegalArgumentException("Falta task_id");
						var task = tasks.findById(taskId)
								.orElseThrow(() -> new IllegalArgumentException("La tarea no existe"));
						validateProject(task.getProjectId(), projectId);

						TaskRequestDTO dto = new TaskRequestDTO();
						dto.setTitle(args.get("title") == null ? task.getTitle() : String.valueOf(args.get("title")));
						dto.setDescription(args.get("description") == null ? task.getDescription()
								: String.valueOf(args.get("description")));
						dto.setDueDate(args.get("due_date") == null ? task.getDueDate()
								: LocalDate.parse(String.valueOf(args.get("due_date"))));
						dto.setAssignedTo(args.get("assigned_to") == null ? task.getAssignedTo()
								: coalesceLong(args.get("assigned_to"), null));
						dto.setSprintId(args.get("sprint_id") == null ? task.getSprintId()
								: coalesceLong(args.get("sprint_id"), null));
						dto.setColor(args.get("color") == null ? task.getColor()
								: String.valueOf(args.get("color")));

						if (dto.getAssignedTo() != null)
							validateMember(projectId, dto.getAssignedTo());
						taskService.update(task.getId(), userId, dto);
						executed.add("Tarea actualizada: " + task.getTitle() + " (id=" + task.getId() + ")");
					}
					case "move_task" -> {
						Long taskId = coalesceLong(args.get("task_id"), null);
						if (taskId == null) throw new IllegalArgumentException("Falta task_id");
						Long columnId = coalesceLong(args.get("column_id"), null);
						if (columnId == null) throw new IllegalArgumentException("Falta column_id");
						var task = tasks.findById(taskId)
								.orElseThrow(() -> new IllegalArgumentException("La tarea no existe"));
						validateProject(task.getProjectId(), projectId);
						validateColumn(projectId, columnId);

						TaskMoveDTO moveDto = new TaskMoveDTO();
						moveDto.setColumnId(columnId);
						moveDto.setPosition(args.get("position") == null ? 0L
								: coalesceLong(args.get("position"), 0L));
						taskService.move(task.getId(), userId, moveDto);
						executed.add("Tarea movida: " + task.getTitle() + " (id=" + task.getId() + ")");
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
					case "update_document" -> {
						Long documentId = coalesceLong(args.get("document_id"), null);
						if (documentId == null) throw new IllegalArgumentException("Falta document_id");
						var document = documents.findByIdAndDeletedAtIsNull(documentId)
								.orElseThrow(() -> new IllegalArgumentException("El documento no existe"));
						validateProject(document.getProjectId(), projectId);

						DocumentUpdateDTO dto = new DocumentUpdateDTO();
						if (args.get("title") != null) dto.setTitle(String.valueOf(args.get("title")));
						if (args.get("content") != null) dto.setContent(String.valueOf(args.get("content")));
						if (args.get("sprint_id") != null) dto.setSprintId(coalesceLong(args.get("sprint_id"), null));

						documentService.update(document.getId(), dto, userId);
						executed.add("Documento actualizado: " + document.getTitle() + " (id=" + document.getId() + ")");
					}
					case "delete_document" -> {
						Long documentId = coalesceLong(args.get("document_id"), null);
						if (documentId == null) throw new IllegalArgumentException("Falta document_id");
						var document = documents.findByIdAndDeletedAtIsNull(documentId)
								.orElseThrow(() -> new IllegalArgumentException("El documento no existe"));
						validateProject(document.getProjectId(), projectId);
						documentService.delete(document.getId());
						executed.add("Documento eliminado: " + document.getTitle() + " (id=" + document.getId() + ")");
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
		List<String> executed = new ArrayList<>();
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

	private TaskRequestDTO taskRequest(AiActionDTO action, TaskEntity task) {
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
		return List.of(
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_project",
						"description", "Consulta el proyecto actual y su contexto.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_project_members",
						"description", "Lista los miembros del proyecto para identificar responsables.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_tasks",
						"description", "Lista tareas del proyecto para revisar estado, fechas y responsables.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "list_documents",
						"description", "Lista documentos y actas para identificar contenido relevante.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_document",
						"description", "Consulta un documento concreto por id.",
						"parameters", java.util.Map.of("type", "object",
								"properties",
								java.util.Map.of("project_id", java.util.Map.of("type", "integer"), "document_id",
										java.util.Map.of("type", "integer")),
								"required", List.of("project_id", "document_id"), "additionalProperties",
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
								"required", List.of("project_id", "title"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "update_document",
						"description", "Actualiza el título, contenido o sprint de un documento existente. Requiere document_id. Usar SIEMPRE que el usuario pida editar, modificar o añadir contenido a un documento.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"document_id", java.util.Map.of("type", "integer"),
										"title", java.util.Map.of("type", "string"),
										"content", java.util.Map.of("type", "string"),
										"sprint_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id", "document_id"), "additionalProperties",
								false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "delete_document",
						"description", "Elimina un documento. Solo usar cuando el usuario lo pida EXPLÍCITAMENTE con palabras como 'elimina', 'borra', 'quita'.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"document_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id", "document_id"), "additionalProperties",
								false))),
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
								"required", List.of("project_id", "title"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "update_task",
						"description", "Actualiza el título, descripción, fecha o responsable de una tarea existente. Requiere task_id.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"task_id", java.util.Map.of("type", "integer"),
										"title", java.util.Map.of("type", "string"),
										"description", java.util.Map.of("type", "string"),
										"due_date", java.util.Map.of("type", "string", "format", "date"),
										"assigned_to", java.util.Map.of("type", "integer"),
										"sprint_id", java.util.Map.of("type", "integer"),
										"color", java.util.Map.of("type", "string")),
								"required", List.of("project_id", "task_id"), "additionalProperties",
								false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "move_task",
						"description", "Mueve una tarea a otra columna del tablero. Requiere task_id y column_id.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of(
										"project_id", java.util.Map.of("type", "integer"),
										"task_id", java.util.Map.of("type", "integer"),
										"column_id", java.util.Map.of("type", "integer"),
										"position", java.util.Map.of("type", "integer")),
								"required", List.of("project_id", "task_id", "column_id"),
								"additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "get_board_columns",
						"description", "Lista columnas del tablero para reutilizar o crear estados adecuados.",
						"parameters", java.util.Map.of("type", "object",
								"properties", java.util.Map.of("project_id", java.util.Map.of("type", "integer")),
								"required", List.of("project_id"), "additionalProperties", false))),
				java.util.Map.of("type", "function", "function", java.util.Map.of(
						"name", "create_board_column",
						"description", "Crea una columna en el proyecto si no existe una compatible.",
						"parameters", java.util.Map.of("type", "object",
								"properties",
								java.util.Map.of("project_id", java.util.Map.of("type", "integer"), "name",
										java.util.Map.of("type", "string"), "color",
										java.util.Map.of("type", "string")),
								"required", List.of("project_id", "name"), "additionalProperties", false))));
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
		return "MEETING_MINUTES".equalsIgnoreCase(value) ? DocumentTypeEnum.MEETING_MINUTES
				: DocumentTypeEnum.DOCUMENT;
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