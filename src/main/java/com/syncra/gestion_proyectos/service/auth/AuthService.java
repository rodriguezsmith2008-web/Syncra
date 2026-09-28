package com.syncra.gestion_proyectos.service.auth;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.users.HttpGlobalResponse;
import com.syncra.gestion_proyectos.dto.users.UserLoginDTO;
import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {

    /**
     * Repositorio de usuarios
     */
    private final UsersRepository usersRepository;

    /**
     * Encriptación de contraseñas
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Servicio de JWT
     */
    private final JwtService jwtService;

    /**
     * Registro de un nuevo usuario en el sistema
     *
     * @param request
     * @return UserMessage con el resultado de la operación
     */
    public UserMessage register(UserRequestDTO request) {

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
        user.setAvatarUrl(request.getAvatarUrl());

        RoleUserEnum role = resolveRole(request);
        user.setRole(role);
        user.setGroupName(role == RoleUserEnum.INSTRUCTOR ? null : request.getGroupName());

        // El usuario queda pendiente de activación
        user.setStatus(UserStatusEnum.IN_TRAINING);

        usersRepository.save(user);

        response.setUserMessage(
                "Registro exitoso. Tu cuenta está pendiente de activación por un administrador");

        return response;
    }

    private RoleUserEnum resolveRole(UserRequestDTO request) {
        return request.getGroupName() == null || request.getGroupName().isBlank()
                ? RoleUserEnum.INSTRUCTOR
                : RoleUserEnum.APPRENTICE;
    }

    /**
     * Inicio de sesión del usuario y generación del token JWT
     *
     * @param request
     * @return HttpGlobalResponse con el token JWT si las credenciales son correctas
     */
    public HttpGlobalResponse<String> login(UserLoginDTO request) {

        HttpGlobalResponse<String> response = new HttpGlobalResponse<>();

        Optional<UsersEntity> userFound = usersRepository.findByEmail(request.getEmail());

        if (userFound.isEmpty()) {
            response.setMessage("Este usuario no se encuentra registrado");
            return response;
        }

        UsersEntity user = userFound.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            response.setMessage("Correo o contraseña incorrectos");
            return response;
        }

        // Validar que la cuenta no esté retirada
        if (user.getStatus() == UserStatusEnum.WITHDRAWN) {
            response.setMessage("Tu cuenta ha sido retirada. Contacta al administrador");
            return response;
        }

        // Validar si la contraseña temporal expiró (pasaron más de 24h sin iniciar
        // sesión)
        if (Boolean.TRUE.equals(user.getMustChangePassword())
                && user.getTempPasswordExpiresAt() != null
                && java.time.LocalDateTime.now().isAfter(user.getTempPasswordExpiresAt())) {

            
            user.setStatus(UserStatusEnum.WITHDRAWN);
            // Genera una contraseña aleatoria encriptada que nadie podrá usar
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            usersRepository.save(user);
            usersRepository.save(user);

            response.setMessage("Tu contraseña temporal expiró. Contacta al administrador para reactivar tu cuenta");
            return response;
        }

        String jwt = jwtService.generarToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name());

        // Si debe cambiar la contraseña, avisarle en la respuesta
        if (Boolean.TRUE.equals(user.getMustChangePassword())) {
            response.setMessage("Inicio de sesión exitoso. Debes cambiar tu contraseña");
            response.setMustChangePassword(true);
        } else {
            response.setMessage("Inicio de sesión exitoso");
        }

        response.setData(jwt);
        return response;
    }

    /**
     * Refresco del token JWT a partir de uno existente que aún no ha expirado
     *
     * @param token
     * @return HttpGlobalResponse con el nuevo token JWT renovado
     */
    public HttpGlobalResponse<String> refreshToken(String token) {

        HttpGlobalResponse<String> response = new HttpGlobalResponse<>();

        try {

            String newJwt = jwtService.refreshToken(token);

            response.setMessage("Token renovado correctamente");
            response.setData(newJwt);

        } catch (Exception e) {

            log.error("Error al refrescar token: {}", e.getMessage());

            response.setMessage("Token inválido o expirado");
        }

        return response;
    }
}