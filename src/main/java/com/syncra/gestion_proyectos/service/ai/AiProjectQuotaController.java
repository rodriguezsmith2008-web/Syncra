package com.syncra.gestion_proyectos.service.ai;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.entity.ai.AiProjectQuotaEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequireRole(RoleUserEnum.APPRENTICE)
@RequiredArgsConstructor
public class AiProjectQuotaController {
    private final AiQuotaService quotaService;

    @GetMapping("/quotas")
    public ResponseEntity<List<AiProjectQuotaEntity>> getProjectQuotas(@RequestParam(required = false) Long projectId, HttpServletRequest request) {
        Long currentProjectId = projectId != null ? projectId : null;
        if (currentProjectId == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(quotaService.getProjectQuotas(currentProjectId));
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
