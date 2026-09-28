package com.syncra.gestion_proyectos.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.syncra.gestion_proyectos.enums.RoleUserEnum;;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    
    RoleUserEnum[] value();

}
