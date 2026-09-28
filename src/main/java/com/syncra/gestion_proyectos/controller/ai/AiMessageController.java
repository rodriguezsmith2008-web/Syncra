package com.syncra.gestion_proyectos.controller.ai;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.syncra.gestion_proyectos.dto.ai.AiMessageRequestDTO;
import com.syncra.gestion_proyectos.dto.ai.AiMessageResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.ai.AiMessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai/conversations/{conversationId}/messages")
@RequireRole(RoleUserEnum.APPRENTICE)
@RequiredArgsConstructor
public class AiMessageController {
	private final AiMessageService service;

	@GetMapping
	public ResponseEntity<List<AiMessageResponseDTO>> list(
			@PathVariable Long conversationId, HttpServletRequest request) {
		return ResponseEntity.ok(service.list(conversationId, userId(request)));
	}

	@PostMapping
	public ResponseEntity<AiMessageResponseDTO> send(
			@PathVariable Long conversationId,
			@Valid @RequestBody AiMessageRequestDTO body,
			HttpServletRequest request) {
		return ResponseEntity.ok(service.send(conversationId, userId(request), body.getMessage()));
	}

	private Long userId(HttpServletRequest request) {
		return (Long) request.getAttribute("userId");
	}
}
