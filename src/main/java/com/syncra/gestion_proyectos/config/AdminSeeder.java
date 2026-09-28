package com.syncra.gestion_proyectos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class AdminSeeder implements CommandLineRunner {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    public void run(String... args) {

        boolean existeAdmin = !usersRepository.findByRole(RoleUserEnum.ADMIN).isEmpty();

        if (existeAdmin) {
            return;
        }

        UsersEntity admin = new UsersEntity();
        admin.setFirstName("Admin");
        admin.setLastName("Syncra");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setDocumentNumber("0000000000");
        admin.setRole(RoleUserEnum.ADMIN);
        admin.setStatus(UserStatusEnum.ACTIVE);

        usersRepository.save(admin);

        log.info("Admin creado por defecto: {}", adminEmail);
    }
}