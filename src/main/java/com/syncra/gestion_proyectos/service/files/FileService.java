package com.syncra.gestion_proyectos.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.syncra.gestion_proyectos.dto.files.FileResponseDTO;
import com.syncra.gestion_proyectos.entity.files.FilesEntity;
import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;
import com.syncra.gestion_proyectos.enums.FilesTypeEnum;
import com.syncra.gestion_proyectos.repository.files.FileRepository;
import com.syncra.gestion_proyectos.service.activity.ActivityLogService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository repository;
    private final Cloudinary cloudinary;
    private final ActivityLogService activityLogService;

    /**
     * Obtiene todos los archivos de un proyecto
     *
     * @param projectId
     * @return lista de archivos
     */
    public List<FileResponseDTO> getByProject(Long projectId) {

        List<FilesEntity> files = repository.findByProjectId(projectId);
        List<FileResponseDTO> response = new ArrayList<>();

        for (FilesEntity file : files) {

            FileResponseDTO dto = new FileResponseDTO();

            dto.setId(file.getId());
            dto.setProjectId(file.getProjectId());
            dto.setName(file.getName());
            dto.setUrl(file.getUrl());
            dto.setType(file.getType());
            dto.setUploadedBy(file.getUploadedBy());

            response.add(dto);
        }

        return response;
    }

    /**
     * Sube un archivo a Cloudinary y guarda su registro en el proyecto
     *
     * @param projectId
     * @param uploadedBy id del usuario que sube el archivo
     * @param file archivo recibido en la peticion (multipart/form-data)
     * @return archivo creado
     */
    @Transactional
    public FileResponseDTO upload(Long projectId, Long uploadedBy, MultipartFile file) {

        String url;
        String originalFilename = file.getOriginalFilename();

        try {
            String safeBaseName = originalFilename == null || originalFilename.isBlank()
                    ? "archivo"
                    : originalFilename.contains(".")
                        ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                        : originalFilename;
            String normalizedBaseName = safeBaseName.replaceAll("[^a-zA-Z0-9_-]", "-");
                boolean raw = esArchivoRaw(originalFilename, file.getContentType())
                    || esPdf(originalFilename, file.getContentType());
                String extension = raw ? obtenerExtension(originalFilename, file.getContentType()) : "";
                String publicId = "syncra/projects/" + projectId + "/" + normalizedBaseName + "-" + UUID.randomUUID()
                    + extension;

            Map<String, Object> uploadOptions = new java.util.HashMap<>();
                uploadOptions.put("resource_type", raw ? "raw" : "auto");
            uploadOptions.put("public_id", publicId);

            Map<?, ?> resultado = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            url = resultado.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo a Cloudinary", e);
        }

        FilesEntity entity = new FilesEntity();
        entity.setProjectId(projectId);
        entity.setName(originalFilename);
        entity.setUrl(url);
        entity.setType(resolverTipo(file.getContentType()));
        entity.setUploadedBy(uploadedBy);

        repository.save(entity);

        activityLogService.log(projectId, ActivityEntityTypeEnum.FILE, entity.getId(),
                ActivityActionEnum.CREATED, "subió el archivo \"" + entity.getName() + "\"", uploadedBy);

        FileResponseDTO dto = new FileResponseDTO();

        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setName(entity.getName());
        dto.setUrl(entity.getUrl());
        dto.setType(entity.getType());
        dto.setUploadedBy(entity.getUploadedBy());

        return dto;
    }

    public String uploadImageFromUrl(Long projectId, String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("La URL debe usar http o https");
            }

            InetAddress address = InetAddress.getByName(uri.getHost());
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                throw new IllegalArgumentException("La URL no es accesible");
            }

            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestProperty("User-Agent", "Syncra/1.0");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
                throw new IOException("La imagen no se pudo descargar");
            }

            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
                throw new IllegalArgumentException("La URL no apunta a una imagen");
            }

            int contentLength = connection.getContentLength();
            if (contentLength > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("La imagen supera los 10 MB");
            }

            byte[] bytes;
            try (InputStream input = connection.getInputStream()) {
                bytes = input.readNBytes(10 * 1024 * 1024 + 1);
            } finally {
                connection.disconnect();
            }

            if (bytes.length > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("La imagen supera los 10 MB");
            }

            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", "image",
                    "public_id", "syncra/projects/" + projectId + "/url-image-" + UUID.randomUUID()));
            return String.valueOf(result.get("secure_url"));
        } catch (Exception exception) {
            throw new IllegalArgumentException("No se pudo subir la imagen desde la URL", exception);
        }
    }

    private String obtenerExtension(String filename, String contentType) {
        if (filename != null) {
            int punto = filename.lastIndexOf('.');
            if (punto >= 0 && punto < filename.length() - 1) {
                return filename.substring(punto).toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9.]", "");
            }
        }

        String tipo = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (tipo) {
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

    private boolean esArchivoRaw(String filename, String contentType) {
        String normalizedFilename = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        return normalizedFilename.endsWith(".xlsx")
                || normalizedFilename.endsWith(".xls")
                || normalizedFilename.endsWith(".pptx")
                || normalizedFilename.endsWith(".ppt")
                || normalizedContentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || normalizedContentType.equals("application/vnd.ms-excel")
                || normalizedContentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                || normalizedContentType.equals("application/vnd.ms-powerpoint");
    }

    private boolean esPdf(String filename, String contentType) {
        String normalizedFilename = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        return normalizedFilename.endsWith(".pdf") || normalizedContentType.equals("application/pdf");
    }

    /**
     * Elimina un archivo del proyecto (no borra el archivo en Cloudinary,
     *
     * @param projectId
     * @param fileId
     */
    @Transactional
    public void delete(Long projectId, Long fileId) {
        delete(projectId, fileId, null);
    }

    /**
     * Elimina un archivo del proyecto, registrando quién lo hizo
     *
     * @param projectId
     * @param fileId
     * @param deletedBy id del usuario que elimina el archivo
     */
    @Transactional
    public void delete(Long projectId, Long fileId, Long deletedBy) {

        FilesEntity entity = repository.findById(fileId).orElse(null);

        if (entity == null || !entity.getProjectId().equals(projectId)) {
            return;
        }

        String name = entity.getName();

        repository.delete(entity);

        if (deletedBy != null) {
            activityLogService.log(projectId, ActivityEntityTypeEnum.FILE, fileId,
                    ActivityActionEnum.DELETED, "eliminó el archivo \"" + name + "\"", deletedBy);
        }
    }

    @Transactional
    public FileResponseDTO updateName(Long projectId, Long fileId, String name) {
        FilesEntity entity = repository.findById(fileId).orElseThrow();

        if (!entity.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("El archivo no pertenece al proyecto");
        }

        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede estar vacio");
        }

        entity.setName(trimmedName);
        repository.save(entity);

        FileResponseDTO dto = new FileResponseDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setName(entity.getName());
        dto.setUrl(entity.getUrl());
        dto.setType(entity.getType());
        dto.setUploadedBy(entity.getUploadedBy());
        return dto;
    }

    /**
     * Determina el tipo de archivo (IMAGE, DOCUMENT, OTHER) segun su content-type
     *
     * @param contentType
     * @return tipo de archivo
     */
    private FilesTypeEnum resolverTipo(String contentType) {

        if (contentType == null) {
            return FilesTypeEnum.OTHER;
        }

        if (contentType.startsWith("image/")) {
            return FilesTypeEnum.IMAGE;
        }

        if (contentType.equals("application/pdf")
                || contentType.contains("word")
                || contentType.contains("excel")
                || contentType.contains("presentation")) {
            return FilesTypeEnum.DOCUMENT;
        }

        return FilesTypeEnum.OTHER;
    }
}