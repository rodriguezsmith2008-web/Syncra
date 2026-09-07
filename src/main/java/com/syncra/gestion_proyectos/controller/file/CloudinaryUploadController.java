package com.syncra.gestion_proyectos.controller.file;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class CloudinaryUploadController {

    private final Cloudinary cloudinary;

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.APPRENTICE, RoleUserEnum.INSTRUCTOR })
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo debe ser una imagen válida."));
        }

        return upload(file, 10 * 1024 * 1024);
    }

    @RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.APPRENTICE, RoleUserEnum.INSTRUCTOR })
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return upload(file, 15 * 1024 * 1024);
    }

    private ResponseEntity<Map<String, String>> upload(MultipartFile file, long maxBytes) {
        if (file.isEmpty() || file.getSize() > maxBytes) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no es válido o supera el tamaño permitido."));
        }

        try {
            String filename = file.getOriginalFilename() == null ? "archivo" : file.getOriginalFilename();
            String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
            String baseName = filename.contains(".")
                    ? filename.substring(0, filename.lastIndexOf('.'))
                    : filename;
            String sanitizedBaseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "-");
                boolean raw = esRaw(filename, contentType);
                String extension = raw ? obtenerExtension(filename, contentType) : "";
                String publicId = "syncra/uploads/" + sanitizedBaseName + "-" + UUID.randomUUID() + extension;
            Map<String, Object> options = ObjectUtils.asMap(
                    "resource_type", raw ? "raw" : "auto",
                    "public_id", publicId);
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            return ResponseEntity.ok(Map.of(
                    "name", filename,
                    "url", String.valueOf(result.get("secure_url"))));
        } catch (IOException exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo subir el archivo a Cloudinary."));
        }
    }

    private boolean esRaw(String filename, String contentType) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || contentType.equals("application/pdf")
                || filename.toLowerCase(Locale.ROOT).matches(".*\\.(doc|docx|xls|xlsx|ppt|pptx)$");
    }

    private String obtenerExtension(String filename, String contentType) {
        int punto = filename.lastIndexOf('.');
        if (punto >= 0 && punto < filename.length() - 1) {
            return filename.substring(punto).toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9.]", "");
        }

        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            default -> "";
        };
    }
}