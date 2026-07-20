package com.syncra.gestion_proyectos.controller.file;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.syncra.gestion_proyectos.dto.files.FileResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.files.FileService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    /**
     * Obtiene todos los archivos de un proyecto
     *
     * @param projectId
     * @return lista de archivos
     */
    @GetMapping
    public ResponseEntity<List<FileResponseDTO>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(service.getByProject(projectId));
    }

    /**
     * Sube un archivo a un proyecto (multipart/form-data)
     *
     * @param projectId
     * @param file
     * @param request usado para obtener el id del usuario autenticado
     * @return archivo creado
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<FileResponseDTO> upload(@PathVariable Long projectId,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(projectId, userId, file));
    }

    /**
     * Elimina un archivo de un proyecto
     *
     * @param projectId
     * @param fileId
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long fileId) {
        service.delete(projectId, fileId);
        return ResponseEntity.noContent().build();
    }
}