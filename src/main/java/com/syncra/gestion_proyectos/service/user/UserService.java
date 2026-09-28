package com.syncra.gestion_proyectos.service.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.syncra.gestion_proyectos.dto.users.FileUploadResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.dto.users.UserResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateMeDTO;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /**
     * Repositorio de usuarios
     */
    private final UsersRepository usersRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Encriptador de contraseñas
     */
    private final PasswordEncoder passwordEncoder;

     private final Cloudinary cloudinary;

    /**
     * Lista todos los usuarios
     *
     * @return lista de usuarios
     */
    public List<UserResponseDTO> listUsers() {

        List<UsersEntity> usersFound = usersRepository.findAll();
        List<UserResponseDTO> response = new ArrayList<>();

        for (UsersEntity user : usersFound) {

            UserResponseDTO dto = new UserResponseDTO();

            dto.setId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setDocumentNumber(user.getDocumentNumber());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole().name());
            dto.setGroupName(user.getGroupName());
            dto.setAvatarUrl(user.getAvatarUrl());
            dto.setStatus(user.getStatus().name());
            dto.setHasSeenOnboarding(Boolean.TRUE.equals(user.getHasSeenOnboarding()));

            response.add(dto);
        }

        return response;
    }

    /**
     * Busca un usuario por id
     *
     * @param id
     * @return usuario encontrado
     */
    public UserResponseDTO getUserById(Long id) {

        Optional<UsersEntity> userFound = usersRepository.findById(id);

        if (userFound.isEmpty()) {
            return null;
        }

        UsersEntity user = userFound.get();

        UserResponseDTO response = new UserResponseDTO();

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setDocumentNumber(user.getDocumentNumber());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setGroupName(user.getGroupName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus().name());
        response.setHasProject(user.getRole() == RoleUserEnum.APPRENTICE
            ? projectMemberRepository.existsByIdUserId(user.getId())
            : null);
        response.setHasSeenOnboarding(Boolean.TRUE.equals(user.getHasSeenOnboarding()));

        return response;
    }

    public UserResponseDTO markOnboardingSeen(Long id) {
        UsersEntity user = usersRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        user.setHasSeenOnboarding(true);
        usersRepository.save(user);
        return getUserById(id);
    }

    /**
     * Crea un usuario desde el panel administrativo
     *
     * @param request
     * @return mensaje de respuesta
     */
    public UserMessage createUser(UserRequestDTO request) {

        UserMessage response = new UserMessage();

        if (usersRepository.existsByEmail(request.getEmail())) {

            response.setUserMessage("El correo ya está en uso");
            return response;
        }

        UsersEntity user = new UsersEntity();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDocumentNumber(request.getDocumentNumber());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGroupName(request.getGroupName());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setAvatarPublicId(request.getAvatarPublicId());

        // ==============================
        // Rol
        // ==============================
        if (request.getRole() != null) {

            try {

                user.setRole(RoleUserEnum.valueOf(request.getRole().toUpperCase()));

            } catch (IllegalArgumentException e) {

                response.setUserMessage("Rol inválido: " + request.getRole());
                return response;

            }

        }

        // ==============================
        // Estado
        // ==============================
        if (request.getStatus() != null) {

            try {

                user.setStatus(UserStatusEnum.valueOf(request.getStatus().toUpperCase()));

            } catch (IllegalArgumentException e) {

                response.setUserMessage("Estado inválido: " + request.getStatus());
                return response;

            }

        }

        usersRepository.save(user);

        response.setUserMessage("Usuario creado correctamente");

        return response;
    }

    /**
     * Actualiza un usuario existente
     *
     * @param id
     * @param request
     * @return mensaje de respuesta
     */
    public UserMessage updateUser(Long id, UserUpdateDTO request) {

        UserMessage response = new UserMessage();

        Optional<UsersEntity> userFound = usersRepository.findById(id);

        if (userFound.isEmpty()) {

            response.setUserMessage("Usuario no encontrado");
            return response;
        }

        UsersEntity user = userFound.get();

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getDocumentNumber() != null)
            user.setDocumentNumber(request.getDocumentNumber());
        if (request.getGroupName() != null)
            user.setGroupName(request.getGroupName());
        if (request.getAvatarUrl() != null
                && !java.util.Objects.equals(request.getAvatarUrl(), user.getAvatarUrl())) {
            if (user.getAvatarPublicId() != null) {
                try {
                    cloudinary.uploader().destroy(user.getAvatarPublicId(),
                            ObjectUtils.asMap("resource_type", "image"));
                } catch (Exception exception) {
                    log.warn("No se pudo eliminar el avatar {} de Cloudinary", user.getAvatarPublicId(), exception);
                }
            }
            user.setAvatarUrl(request.getAvatarUrl());
            user.setAvatarPublicId(request.getAvatarPublicId());
        }

        if (request.getStatus() != null) {
            UserStatusEnum newStatus = null;
            for (UserStatusEnum s : UserStatusEnum.values()) {
                if (s.name().equalsIgnoreCase(request.getStatus())) {
                    newStatus = s;
                    break;
                }
            }
            if (newStatus == null) {
                response.setUserMessage("Estado inválido: " + request.getStatus());
                return response;
            }
            user.setStatus(newStatus);
        }

        if (request.getRole() != null) {

            RoleUserEnum newRole = null;

            for (RoleUserEnum r : RoleUserEnum.values()) {
                if (r.name().equalsIgnoreCase(request.getRole())) {
                    newRole = r;
                    break;
                }
            }

            if (newRole == null) {
                response.setUserMessage("Rol inválido: " + request.getRole());
                return response;
            }

            if (user.getRole() == RoleUserEnum.ADMIN
                    && newRole != RoleUserEnum.ADMIN
                    && usersRepository.findByRole(RoleUserEnum.ADMIN).size() <= 1) {

                response.setUserMessage("No se puede cambiar el rol: debe existir al menos un administrador");
                return response;
            }

            user.setRole(newRole);
        }

        usersRepository.save(user);

        response.setUserMessage("Usuario actualizado correctamente");

        return response;
    }

    /**
     * Elimina un usuario
     *
     * @param id
     * @return mensaje de respuesta
     */
    public UserMessage deleteUser(Long id) {

        UserMessage response = new UserMessage();

        Optional<UsersEntity> userFound = usersRepository.findById(id);

        if (userFound.isEmpty()) {
            response.setUserMessage("Usuario no encontrado");
            return response;
        }

        UsersEntity user = userFound.get();

        // Validar que no sea el último admin
        if (user.getRole() == RoleUserEnum.ADMIN
                && usersRepository.findByRole(RoleUserEnum.ADMIN).size() <= 1) {
            response.setUserMessage("No se puede eliminar el único administrador del sistema");
            return response;
        }

        // Eliminación lógica: cambia el estado en vez de borrar de la BD
        user.setStatus(UserStatusEnum.WITHDRAWN);
        usersRepository.save(user);

        response.setUserMessage("Usuario desactivado correctamente");
        return response;
    }

    /**
     * Busca usuarios según un criterio
     *
     * @param type     tipo de búsqueda: "rol", "document" o "name"
     * @param criterio texto a buscar según el tipo
     * @return lista de usuarios encontrados (vacía si no hay coincidencias)
     */
    public List<UserResponseDTO> searchUsers(String type, String criterio) {

        switch (type) {

            case "rol":
                try {
                    RoleUserEnum role = RoleUserEnum.valueOf(criterio.toUpperCase());
                    return userSearchRole(role);
                } catch (IllegalArgumentException e) {
                    return new ArrayList<>();
                }

            case "document":
                UserResponseDTO userFound = userSearchDocumentNUmber(criterio);
                List<UserResponseDTO> documentResult = new ArrayList<>();
                if (userFound != null) {
                    documentResult.add(userFound);
                }
                return documentResult;

            case "name":
                return userSearchName(criterio);

            default:
                return new ArrayList<>();
        }
    }

    /**
     * Busca usuarios cuyo nombre o apellido contengan el texto indicado
     *
     * @param nameOrLasname texto a buscar en nombre o apellido
     * @return lista de usuarios encontrados
     */
    private List<UserResponseDTO> userSearchName(String nameOrLasname) {

        List<UsersEntity> entities = usersRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameOrLasname, nameOrLasname);
        List<UserResponseDTO> dtos = new ArrayList<>();

        for (UsersEntity userfound : entities) {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setId(userfound.getId());
            dto.setFirstName(userfound.getFirstName());
            dto.setLastName(userfound.getLastName());
            dto.setDocumentNumber(userfound.getDocumentNumber());
            dto.setEmail(userfound.getEmail());
            dto.setRole(userfound.getRole().name());
            dto.setGroupName(userfound.getGroupName());
            dto.setAvatarUrl(userfound.getAvatarUrl());
            dto.setStatus(userfound.getStatus().name());
            dto.setHasSeenOnboarding(Boolean.TRUE.equals(userfound.getHasSeenOnboarding()));

            dtos.add(dto);
        }
        return dtos;
    }

    /**
     * Busca un usuario por su número de documento exacto
     *
     * @param documentNumber número de documento
     * @return usuario encontrado, o null si no existe
     */
    private UserResponseDTO userSearchDocumentNUmber(String documentNumber) {

        Optional<UsersEntity> userFound = usersRepository.findByDocumentNumber(documentNumber);

        if (userFound.isEmpty()) {
            return null;
        }

        UsersEntity user = userFound.get();

        UserResponseDTO response = new UserResponseDTO();

        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setDocumentNumber(user.getDocumentNumber());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setGroupName(user.getGroupName());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setStatus(user.getStatus().name());
        response.setHasSeenOnboarding(Boolean.TRUE.equals(user.getHasSeenOnboarding()));

        return response;
    }

    /**
     * Busca todos los usuarios que tienen el rol indicado
     *
     * @param role rol a filtrar
     * @return lista de usuarios con ese rol
     */
    private List<UserResponseDTO> userSearchRole(RoleUserEnum role) {

        List<UsersEntity> entities = usersRepository.findByRole(role);
        List<UserResponseDTO> dtos = new ArrayList<>();

        for (UsersEntity userfound : entities) {
            UserResponseDTO dto = new UserResponseDTO();
            dto.setId(userfound.getId());
            dto.setFirstName(userfound.getFirstName());
            dto.setLastName(userfound.getLastName());
            dto.setDocumentNumber(userfound.getDocumentNumber());
            dto.setEmail(userfound.getEmail());
            dto.setRole(userfound.getRole().name());
            dto.setGroupName(userfound.getGroupName());
            dto.setAvatarUrl(userfound.getAvatarUrl());
            dto.setStatus(userfound.getStatus().name());
            dto.setHasSeenOnboarding(Boolean.TRUE.equals(userfound.getHasSeenOnboarding()));

            dtos.add(dto);
        }
        return dtos;
    }

   

    /**
     * Sube el avatar a Cloudinary en la carpeta "avatars"
     *
     * @param file imagen enviada desde el front
     * @return DTO con la url resultante
     */
    public FileUploadResponseDTO uploadAvatar(MultipartFile file) throws IOException {

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "avatars",
                        "resource_type", "image"));

        String url = (String) uploadResult.get("secure_url");
        String publicId = String.valueOf(uploadResult.get("public_id"));

        return new FileUploadResponseDTO(url, publicId);
    }

    /**
     * Actualiza el perfil del usuario autenticado
     *
     * @param id     id del usuario autenticado (extraído del JWT)
     * @param update datos a actualizar
     * @return mensaje de respuesta
     */
   public UserMessage updateProfile(Long id, UserUpdateMeDTO update) {

    UserMessage message = new UserMessage();
    Optional<UsersEntity> user = usersRepository.findById(id);

    if (user.isEmpty()) {
        message.setUserMessage("usuario no encontrado");
        return message;
    }

    UsersEntity usersEntity = user.get();

    if (update.getEmail() != null && !update.getEmail().equals(usersEntity.getEmail())) {
        if (usersRepository.existsByEmail(update.getEmail())) {
            message.setUserMessage("Correo ya existente");
            return message;
        }
        usersEntity.setEmail(update.getEmail());
    }

    if (update.getFirstName() != null)
        usersEntity.setFirstName(update.getFirstName());
    if (update.getLastName() != null)
        usersEntity.setLastName(update.getLastName());
    if (update.getDocumentNumber() != null)
        usersEntity.setDocumentNumber(update.getDocumentNumber());

    if (!java.util.Objects.equals(update.getAvatarUrl(), usersEntity.getAvatarUrl())) {
        if (usersEntity.getAvatarPublicId() != null) {
            try {
                cloudinary.uploader().destroy(usersEntity.getAvatarPublicId(),
                        ObjectUtils.asMap("resource_type", "image"));
            } catch (Exception exception) {
                log.warn("No se pudo eliminar el avatar {} de Cloudinary", usersEntity.getAvatarPublicId(), exception);
            }
        }

        usersEntity.setAvatarPublicId(update.getAvatarPublicId());
        usersEntity.setAvatarUrl(update.getAvatarUrl());
    }

    usersRepository.save(usersEntity);
    message.setUserMessage("Actualización exitosa");
    return message;
}

    /**
     * Cambia la contraseña del usuario autenticado
     *
     * @param id          id del usuario autenticado (extraído del JWT)
     * @param currentPass contraseña actual
     * @param newPass     nueva contraseña
     * @return mensaje de respuesta
     */
    public UserMessage verifyCurrentPassword(Long id, String currentPass) {

        UserMessage message = new UserMessage();

        if (currentPass == null || currentPass.isBlank()) {
            throw new IllegalArgumentException("Debes ingresar tu contraseña actual.");
        }

        Optional<UsersEntity> userFound = usersRepository.findById(id);
        if (userFound.isEmpty()) {
            throw new EntityNotFoundException("Usuario no encontrado");
        }

        UsersEntity user = userFound.get();

        if (!passwordEncoder.matches(currentPass, user.getPassword())) {
            throw new BadCredentialsException("La contraseña actual es incorrecta");
        }

        message.setUserMessage("Contraseña actual correcta");
        return message;
    }

    public UserMessage changePassword(Long id, String currentPass, String newPass) {

    UserMessage message = new UserMessage();

    Optional<UsersEntity> userFound = usersRepository.findById(id);

    if (userFound.isEmpty()) {
        throw new EntityNotFoundException("Usuario no encontrado");
    }

    if (currentPass == null || currentPass.isBlank()) {
        throw new IllegalArgumentException("Debes ingresar tu contraseña actual.");
    }

    if (newPass == null || newPass.isBlank()) {
        throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
    }

    if (newPass.length() < 8) {
        throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
    }

    if (!newPass.matches(".*[0-9].*")) {
        throw new IllegalArgumentException("La nueva contraseña debe incluir al menos un número.");
    }

    String specialChars = "!@#$%^&*()_+=-{}[]:;\"'|,.<>/?~`";
    boolean hasSpecialChar = newPass.chars().anyMatch(ch -> specialChars.indexOf(ch) >= 0);
    if (!hasSpecialChar) {
        throw new IllegalArgumentException("La nueva contraseña debe incluir al menos un carácter especial.");
    }

    UsersEntity user = userFound.get();

    if (!passwordEncoder.matches(currentPass, user.getPassword())) {
        throw new BadCredentialsException("La contraseña actual es incorrecta");
    }

    if (passwordEncoder.matches(newPass, user.getPassword())) {
        throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
    }

    user.setPassword(passwordEncoder.encode(newPass));
    user.setMustChangePassword(false);
    user.setTempPasswordExpiresAt(null);
    usersRepository.save(user);

    message.setUserMessage("Contraseña actualizada correctamente");

    return message;
}

/**
 * Obtiene usuarios filtrados por rol y búsqueda opcional.
 * Si role no se especifica, devuelve todos; si search no se especifica, no filtra por texto.
 *
 * @param role   nombre del rol (p.ej. "APPRENTICE") o null
 * @param search término de búsqueda (nombre, documento o grupo) o null
 * @return lista de usuarios filtrados
 */
public List<UserResponseDTO> getUsersByRoleAndSearch(String role, String search) {
    // 1. Obtener usuarios por rol si se especifica
    List<UsersEntity> users;
    if (role != null && !role.isBlank()) {
        try {
            RoleUserEnum roleEnum = RoleUserEnum.valueOf(role.toUpperCase());
            users = usersRepository.findByRole(roleEnum);
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    } else {
        users = usersRepository.findAll();
    }

    // 2. Si no hay búsqueda, convertir y devolver
    if (search == null || search.isBlank()) {
        return users.stream().map(this::toUserResponseDTO).toList();
    }

    // 3. Filtrar por búsqueda (nombre, documento o grupo)
    String lowerSearch = search.toLowerCase().trim();
    return users.stream()
            .filter(u ->
                    u.getFirstName().toLowerCase().contains(lowerSearch) ||
                    u.getLastName().toLowerCase().contains(lowerSearch) ||
                    u.getDocumentNumber().toLowerCase().contains(lowerSearch) ||
                    (u.getGroupName() != null && u.getGroupName().toLowerCase().contains(lowerSearch))
            )
            .map(this::toUserResponseDTO)
            .toList();
}

/**
 * Método auxiliar para convertir UsersEntity a UserResponseDTO
 */
private UserResponseDTO toUserResponseDTO(UsersEntity user) {
    UserResponseDTO dto = new UserResponseDTO();
    dto.setId(user.getId());
    dto.setFirstName(user.getFirstName());
    dto.setLastName(user.getLastName());
    dto.setDocumentNumber(user.getDocumentNumber());
    dto.setEmail(user.getEmail());
    dto.setRole(user.getRole().name());
    dto.setGroupName(user.getGroupName());
    dto.setAvatarUrl(user.getAvatarUrl());
    dto.setStatus(user.getStatus().name());
    dto.setHasSeenOnboarding(Boolean.TRUE.equals(user.getHasSeenOnboarding()));

    if (user.getRole() == RoleUserEnum.APPRENTICE) {
        boolean hasProject = projectMemberRepository.existsByIdUserId(user.getId());
        dto.setHasProject(hasProject);
    } else {
        dto.setHasProject(null);
    }
    return dto;
}
}