package com.syncra.gestion_proyectos.service.ai;

import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.syncra.gestion_proyectos.dto.ai.AiConversationRequestDTO;
import com.syncra.gestion_proyectos.dto.ai.AiConversationResponseDTO;
import com.syncra.gestion_proyectos.entity.ai.AiConversationEntity;
import com.syncra.gestion_proyectos.repository.ai.AiConversationRepository;
import com.syncra.gestion_proyectos.repository.ai.AiMessageRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiConversationService {
	private final AiConversationRepository conversations;
	private final AiMessageRepository messages;
	private final ProjectMemberRepository members;

	public List<AiConversationResponseDTO> list(Long userId) {
		return conversations.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
	}

	@Transactional
	public AiConversationResponseDTO create(Long userId, AiConversationRequestDTO request) {
		validateProjectAccess(userId, request.getProjectId());
		AiConversationEntity entity = new AiConversationEntity();
		entity.setUserId(userId);
		entity.setProjectId(request.getProjectId());
		entity.setTitle(request.getTitle() == null || request.getTitle().isBlank()
				? "Nueva conversación" : request.getTitle().trim());
		return toResponse(conversations.save(entity));
	}

	public AiConversationEntity owned(Long id, Long userId) {
		return conversations.findByIdAndUserId(id, userId)
				.orElseThrow(() -> new NoSuchElementException("La conversación no existe"));
	}

	@Transactional
	public void delete(Long id, Long userId) {
		AiConversationEntity conversation = owned(id, userId);
		messages.deleteByConversationId(conversation.getId());
		conversations.delete(conversation);
	}

	@Transactional
	public AiConversationResponseDTO attachProject(Long id, Long userId, Long projectId) {
		validateProjectAccess(userId, projectId);
		AiConversationEntity conversation = owned(id, userId);
		conversation.setProjectId(projectId);
		return toResponse(conversations.save(conversation));
	}

	public void validateProjectAccess(Long userId, Long projectId) {
		if (projectId != null && !members.existsByIdProjectIdAndIdUserId(projectId, userId)) {
			throw new AccessDeniedException("No tienes acceso a ese proyecto");
		}
	}

	private AiConversationResponseDTO toResponse(AiConversationEntity entity) {
		AiConversationResponseDTO dto = new AiConversationResponseDTO();
		dto.setId(entity.getId());
		dto.setProjectId(entity.getProjectId());
		dto.setTitle(entity.getTitle());
		dto.setCreatedAt(entity.getCreatedAt());
		dto.setUpdatedAt(entity.getCreatedAt());
		return dto;
	}
}
