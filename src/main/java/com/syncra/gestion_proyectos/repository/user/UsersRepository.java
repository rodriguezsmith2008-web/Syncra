package com.syncra.gestion_proyectos.repository.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.user.UsersEntity;

@Repository
public interface UsersRepository extends JpaRepository<UsersEntity, Long> {

    Optional<UsersEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UsersEntity> findByResetToken(String resetToken);

}