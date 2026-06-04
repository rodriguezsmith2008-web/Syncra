package com.syncra.gestion_proyectos.service.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.Users.UserMessage;
import com.syncra.gestion_proyectos.dto.Users.UserRequestDTO;
import com.syncra.gestion_proyectos.dto.Users.UserResponseDTO;
import com.syncra.gestion_proyectos.dto.Users.UserUpdateDTO;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Repositorio de usuarios
     */
    private final UsersRepository usersRepository;

    /**
     * Encriptador de contraseñas
     */
    private final PasswordEncoder passwordEncoder;

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

        return response;
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

        if (request.getRole() != null) {
            try {
                user.setRole(RoleUserEnum.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                response.setUserMessage("Rol inválido: " + request.getRole());
                return response;
            }
        }

        user.setStatus(UserStatusEnum.IN_TRAINING);

        usersRepository.save(user);

        response.setUserMessage("Usuario creado correctamente");

        return response;
    }

    /**
     * Actualiza un usuario
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

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDocumentNumber(request.getDocumentNumber());
        user.setGroupName(request.getGroupName());
        user.setAvatarUrl(request.getAvatarUrl());

        if (request.getStatus() != null) {
            user.setStatus(UserStatusEnum.valueOf(request.getStatus().toUpperCase()));
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

        usersRepository.deleteById(id);

        response.setUserMessage("Usuario eliminado correctamente");

        return response;
    }
}