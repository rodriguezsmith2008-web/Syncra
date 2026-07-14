package com.syncra.gestion_proyectos.controller.project;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.project.ProjectMemberResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.project.ProjectMemberService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    // Servicio que se encarga de la logica de negocio de los miembros de los
    // proyectos
    private final ProjectMemberService memberService;

    /**
     * Obtiene todos los miembros de un proyecto
     * 
     * @param projectId id del proyecto
     * @return lista de miembros
     */
    @GetMapping
    public ResponseEntity<List<ProjectMemberResponseDTO>> getMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(memberService.getMembers(projectId));
    }

    /**
     * Agrega un usuario como miembro de un proyecto
     * 
     * @param projectId
     * @param userId    id del usuario
     * @return miembro agregado
     */
    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @PostMapping
    public ResponseEntity<ProjectMemberResponseDTO> addMember(@PathVariable Long projectId, @RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.addMember(projectId, userId));
    }

    /**
     * Elimina un miembro de un proyecto
     * 
     * @param projectId
     * @param userId    id del usuario
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.INSTRUCTOR)
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, String>> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        memberService.removeMember(projectId, userId);
        return ResponseEntity.ok(Map.of("message", "Miembro eliminado"));
    }

}
