package com.syncra.gestion_proyectos.controller.ai;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.syncra.gestion_proyectos.dto.ai.AiQuotaStatusDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.ai.AiConversationService;
import com.syncra.gestion_proyectos.service.ai.AiQuotaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequireRole(RoleUserEnum.APPRENTICE)
@RequiredArgsConstructor
public class AiProjectQuotaController {
    private final AiQuotaService quotaService;
    private final AiConversationService conversationService;

    @GetMapping("/quotas")
    public ResponseEntity<List<AiQuotaStatusDTO>> getProjectQuotas(
            @RequestParam(required = false) Long projectId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (projectId == null) return ResponseEntity.ok(List.of());
        conversationService.validateProjectAccess(userId, projectId);
        List<AiQuotaStatusDTO> result = quotaService.modelPriority().stream()
                .map(tier -> {
                    AiQuotaStatusDTO dto = quotaService.toStatus(quotaService.getOrCreateQuota(projectId, tier));
                    dto.setProjectId(projectId);
                    dto.setModelTier(tier.name());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(result);
    }
}