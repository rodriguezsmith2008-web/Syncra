package com.syncra.gestion_proyectos.controller.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.syncra.gestion_proyectos.dto.users.FileUploadResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserChangePasswordDTO;
import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.dto.users.UserResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateMeDTO;
import com.syncra.gestion_proyectos.dto.users.VerifyCurrentPasswordDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.user.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/users")
public class UserController {

    /**
     * Servicio de usuarios
     */
    private final UserService userService;

    /**
     * Lista todos los usuarios registrados
     *
     * @return lista de usuarios
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping("/list-users")
    public ResponseEntity<List<UserResponseDTO>> listUsers() {
        try {
            List<UserResponseDTO> response = userService.listUsers();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Busca un usuario por su id
     *
     * @param id
     * @return usuario encontrado
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping("/get-user/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        try {
            UserResponseDTO response = userService.getUserById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Crea un nuevo usuario desde el panel administrativo
     *
     * @param request datos del nuevo usuario
     * @return mensaje de respuesta
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @PostMapping("/create-user")
    public ResponseEntity<UserMessage> createUser(@RequestBody UserRequestDTO request) {
        try {
            UserMessage response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Actualiza un usuario
     *
     * @param id
     * @param request
     * @return mensaje de respuesta
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @PutMapping("/update-user/{id}")
    public ResponseEntity<UserMessage> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateDTO request) {

        try {
            UserMessage response = userService.updateUser(id, request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Elimina un usuario
     *
     * @param id
     * @return mensaje de respuesta
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<UserMessage> deleteUser(@PathVariable Long id) {
        try {
            UserMessage response = userService.deleteUser(id);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Busca usuarios según un criterio
     *
     * @param type     tipo de búsqueda: "rol", "document" o "name"
     * @param criterio texto a buscar según el tipo
     * @return lista de usuarios encontrados
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping("/search-users")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String type, @RequestParam String criterio) {
        try {
            List<UserResponseDTO> response = userService.searchUsers(type, criterio);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Sube el avatar de un usuario a Cloudinary y devuelve la URL
     *
     * @param file imagen del avatar
     * @return url de la imagen subida
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<FileUploadResponseDTO> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        try {
            FileUploadResponseDTO response = userService.uploadAvatar(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Obtiene el perfil del usuario autenticado
     *
     * @param request
     * @return datos del usuario autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getOwnProfile(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            UserResponseDTO response = userService.getUserById(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PatchMapping("/{id}/onboarding")
    public ResponseEntity<UserResponseDTO> completeOnboarding(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            Long authenticatedUserId = (Long) request.getAttribute("userId");
            if (!id.equals(authenticatedUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
            }

            return ResponseEntity.ok(userService.markOnboardingSeen(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Actualiza el perfil del usuario autenticado
     *
     * @param request
     * @param update
     * @return mensaje de respuesta
     */
    @PutMapping("/me")
    public ResponseEntity<UserMessage> updateOwnProfile(
            HttpServletRequest request,
            @Valid @RequestBody UserUpdateMeDTO update) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            UserMessage response = userService.updateProfile(userId, update);

            if ("Correo ya existente".equals(response.getUserMessage())
                    || "usuario no encontrado".equalsIgnoreCase(response.getUserMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Cambia la contraseña del usuario autenticado
     *
     * @param request
     * @param body
     * @return mensaje de respuesta
     */
   @PutMapping("/me/password")
public ResponseEntity<UserMessage> changePassword(
        HttpServletRequest request,
        @Valid @RequestBody UserChangePasswordDTO body) {

    Long userId = (Long) request.getAttribute("userId");
    UserMessage response = userService.changePassword(userId, body.getCurrentPassword(), body.getNewPassword());
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
}

    @PostMapping("/me/verify-current-password")
    public ResponseEntity<UserMessage> verifyCurrentPassword(
            HttpServletRequest request,
            @Valid @RequestBody VerifyCurrentPasswordDTO body) {

        Long userId = (Long) request.getAttribute("userId");
        UserMessage response = userService.verifyCurrentPassword(userId, body.getCurrentPassword());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
@GetMapping
@RequireRole({ RoleUserEnum.ADMIN, RoleUserEnum.INSTRUCTOR })
public ResponseEntity<List<UserResponseDTO>> getUsers(
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String search) {
    List<UserResponseDTO> result = userService.getUsersByRoleAndSearch(role, search);
    return ResponseEntity.ok(result);
}

}