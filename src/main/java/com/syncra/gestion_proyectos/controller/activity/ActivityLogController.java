package com.syncra.gestion_proyectos.controller.activity;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.activity.ActivityLogResponseDTO;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService service;

    /**
     * Obtiene el historial global de actividad de un proyecto
     * (documentos, recursos, archivos y columnas del tablero)
     *
     * @param projectId
     * @return lista de eventos, más reciente primero
     */
    @GetMapping
    public ResponseEntity<List<ActivityLogResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(projectId));
    }

}