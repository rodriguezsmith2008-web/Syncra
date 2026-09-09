package com.syncra.gestion_proyectos.controller.ai;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.syncra.gestion_proyectos.dto.ai.AiConversationRequestDTO;
import com.syncra.gestion_proyectos.dto.ai.AiConversationResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.ai.AiConversationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai/conversations")
@RequireRole(RoleUserEnum.APPRENTICE)
@RequiredArgsConstructor
public class AiConversationController {
	private final AiConversationService service;

	@GetMapping
	public ResponseEntity<List<AiConversationResponseDTO>> list(HttpServletRequest request) {
		return ResponseEntity.ok(service.list(userId(request)));
	}

	@PostMapping
	public ResponseEntity<AiConversationResponseDTO> create(
			@RequestBody AiConversationRequestDTO body, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId(request), body));
	}

	@DeleteMapping("/{conversationId}")
	public ResponseEntity<Void> delete(@PathVariable Long conversationId, HttpServletRequest request) {
		service.delete(conversationId, userId(request));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{conversationId}/project")
	public ResponseEntity<AiConversationResponseDTO> attachProject(
			@PathVariable Long conversationId, @RequestBody AiConversationRequestDTO body,
			HttpServletRequest request) {
		return ResponseEntity.ok(service.attachProject(conversationId, userId(request), body.getProjectId()));
	}

	private Long userId(HttpServletRequest request) {
		return (Long) request.getAttribute("userId");
	}
}
