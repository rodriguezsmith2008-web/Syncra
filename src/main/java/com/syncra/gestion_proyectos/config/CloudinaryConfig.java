package com.syncra.gestion_proyectos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url}")
    private String cloudinaryUrl;

    /**
     * Crea el bean de Cloudinary a partir de la variable CLOUDINARY_URL,
     * que ya trae api_key, api_secret y cloud_name en un solo string.
     *
     * @return cliente de Cloudinary configurado
     */
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(cloudinaryUrl);
    }
}