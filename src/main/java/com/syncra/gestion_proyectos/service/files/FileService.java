package com.syncra.gestion_proyectos.service.files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.syncra.gestion_proyectos.dto.files.FileResponseDTO;
import com.syncra.gestion_proyectos.entity.files.FilesEntity;
import com.syncra.gestion_proyectos.enums.FilesTypeEnum;
import com.syncra.gestion_proyectos.repository.files.FileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository repository;
    private final Cloudinary cloudinary;

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
            Map<?, ?> uploadOptions = ObjectUtils.asMap("resource_type", "auto");

            if (esArchivoRaw(originalFilename, file.getContentType())) {
                uploadOptions = ObjectUtils.asMap(
                        "resource_type", "raw",
                        "public_id", originalFilename);
            }

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

        FileResponseDTO dto = new FileResponseDTO();

        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setName(entity.getName());
        dto.setUrl(entity.getUrl());
        dto.setType(entity.getType());
        dto.setUploadedBy(entity.getUploadedBy());

        return dto;
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

    /**
     * Elimina un archivo del proyecto (no borra el archivo en Cloudinary,
     *
     * @param projectId
     * @param fileId
     */
    @Transactional
    public void delete(Long projectId, Long fileId) {

        FilesEntity entity = repository.findById(fileId).orElse(null);

        if (entity == null || !entity.getProjectId().equals(projectId)) {
            return;
        }

        repository.delete(entity);
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