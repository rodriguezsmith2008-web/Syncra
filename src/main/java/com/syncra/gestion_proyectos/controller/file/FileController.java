package com.syncra.gestion_proyectos.controller.file;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.syncra.gestion_proyectos.dto.files.FileResponseDTO;
import com.syncra.gestion_proyectos.dto.files.FileUpdateDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.files.FileService;
import java.util.Map;

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
     * Sube una imagen desde el editor de documentos a Cloudinary
     * y retorna solo la URL para que el editor la inserte como <img src="...">
     * Esto evita que las imágenes se guarden como Base64 en el contenido
     * del documento, lo que causaría problemas al generar el PDF
     *
     * @param file imagen recibida del editor
     * @return URL de la imagen en Cloudinary
     */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadEditorImage(
            @PathVariable Long projectId, // ← agregar esto
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El archivo debe ser una imagen válida."));
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La imagen no puede superar los 10 MB."));
            }
            Long userId = (Long) request.getAttribute("userId");
            FileResponseDTO uploaded = service.upload(projectId, userId, file);
            return ResponseEntity.ok(Map.of("url", uploaded.getUrl()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al subir imagen: " + e.getMessage()));
        }
    }

    /**
     * Sube un archivo a un proyecto (multipart/form-data)
     *
     * @param projectId
     * @param file
     * @param request   usado para obtener el id del usuario autenticado
     * @return archivo creado
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @PostMapping
    public ResponseEntity<FileResponseDTO> upload(@PathVariable Long projectId,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(projectId, userId, file));
    }

    @RequireRole(RoleUserEnum.APPRENTICE)
    @PutMapping("/{fileId}")
    public ResponseEntity<FileResponseDTO> updateName(@PathVariable Long projectId,
            @PathVariable Long fileId, @RequestBody FileUpdateDTO request) {
        return ResponseEntity.ok(service.updateName(projectId, fileId, request.getName()));
    }

    /**
     * Elimina un archivo de un proyecto
     *
     * @param projectId
     * @param fileId
     * @param request   usado para obtener el id del usuario autenticado
     * @return respuesta sin contenido
     */
    @RequireRole(RoleUserEnum.APPRENTICE)
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable Long projectId, @PathVariable Long fileId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        service.delete(projectId, fileId, userId);
        return ResponseEntity.noContent().build();
    }
}