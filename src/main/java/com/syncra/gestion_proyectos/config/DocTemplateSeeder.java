package com.syncra.gestion_proyectos.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
@Order(2)
public class DocTemplateSeeder implements CommandLineRunner {

    @Override
    public void run(String... args) {
        // Se removieron las plantillas predefinidas del backend.
        // Las plantillas son gestionadas dinámicamente por los instructores.
    }
}