package com.syncra.gestion_proyectos.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncra.gestion_proyectos.filter.JwtValidationFilter;

@Configuration 
public class FilterConfig {

    @Bean 
    FilterRegistrationBean<JwtValidationFilter> jwtFilter(JwtValidationFilter jwtValidationFilter) {

        // Se crea el objeto que permitirá registrar el filtro
        FilterRegistrationBean<JwtValidationFilter> registrationBean = new FilterRegistrationBean<>();

        // Aquí se asigna el filtro JWT que se va a ejecutar
        registrationBean.setFilter(jwtValidationFilter);

        // Se indica que el filtro se aplicará a todas las rutas del proyecto
        registrationBean.addUrlPatterns("/*");

        // Define el orden de ejecución del filtro
        // Entre menor sea el número, más prioridad tiene
        registrationBean.setOrder(0);

        // Retorna la configuración del filtro ya registrada
        return registrationBean;

    }

}