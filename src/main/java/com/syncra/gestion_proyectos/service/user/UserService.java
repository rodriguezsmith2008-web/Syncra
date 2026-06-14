package com.syncra.gestion_proyectos.service.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.dto.users.UserResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateDTO;
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

 /**
     * Busca usuarios según un criterio (RF9)
     *
     * @param type tipo de búsqueda: "rol", "document" o "name"
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

        List<UsersEntity> entities = usersRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(nameOrLasname, nameOrLasname);
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

            dtos.add(dto);
        }
        return dtos;
    }


}