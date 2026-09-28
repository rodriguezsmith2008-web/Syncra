package com.syncra.gestion_proyectos.repository.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;

public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    /**
     * Busca un usuario por su correo electrónico
     *
     * @param email
     * @return usuario encontrado
     */
    Optional<UsersEntity> findByEmail(String email);

    /**
     * Verifica si ya existe un usuario con ese correo
     *
     * @param email
     * @return true si existe, false si no
     */
    boolean existsByEmail(String email);

    /**
     * Busca un usuario por su token de recuperación de contraseña
     *
     * @param resetToken
     * @return usuario encontrado
     */
    

    /**
     * Busca usuarios cuyo nombre o apellido contengan el texto indicado,
     * sin distinguir mayúsculas/minúsculas (búsqueda por nombre)
     *
     * @param firstname
     * @param lastName
     * @return lista de usuarios que coinciden
     */
    List<UsersEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstname, String lastName);

    /**
     * Busca un usuario por su número de documento ( búsqueda por documento)
     *
     * @param documentNumber
     * @return usuario encontrado
     */
    Optional<UsersEntity> findByDocumentNumber(String documentNumber);

    /**
     * Busca todos los usuarios que tienen el rol indicado (búsqueda por rol)
     *
     * @param role
     * @return lista de usuarios con ese rol
     */
    List<UsersEntity> findByRole(RoleUserEnum role);

    long countByRole(RoleUserEnum role);

    List<UsersEntity> findByStatus(UserStatusEnum status);

    long countByStatus(UserStatusEnum status);
}