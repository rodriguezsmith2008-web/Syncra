package com.syncra.gestion_proyectos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class AppConfig {

    //Sirve para usar el objeto en toda el proyecto
    @Bean
    PasswordEncoder passwordEncoder(){

       //Codifica las contraseñas 
        return new BCryptPasswordEncoder();
    }

}