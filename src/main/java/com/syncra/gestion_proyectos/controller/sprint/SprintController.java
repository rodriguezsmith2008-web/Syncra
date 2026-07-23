package com.syncra.gestion_proyectos.controller.sprint;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.syncra.gestion_proyectos.dto.sprint.SprintRequestDTO;
import com.syncra.gestion_proyectos.dto.sprint.SprintResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.sprint.SprintService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    @PostMapping
    @RequireRole({ RoleUserEnum.APPRENTICE, RoleUserEnum.INSTRUCTOR })
    public ResponseEntity<SprintResponseDTO> create(@RequestBody SprintRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.create(dto));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(sprintService.getByProject(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SprintResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.getById(id));
    }

    @PutMapping("/{id}")
    @RequireRole({ RoleUserEnum.APPRENTICE, RoleUserEnum.INSTRUCTOR })
    public ResponseEntity<SprintResponseDTO> update(@PathVariable Long id,
            @RequestBody SprintRequestDTO dto) {
        return ResponseEntity.ok(sprintService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @RequireRole({ RoleUserEnum.APPRENTICE, RoleUserEnum.INSTRUCTOR })
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        sprintService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Sprint eliminado correctamente"));
    }

}