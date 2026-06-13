package com.syncra.gestion_proyectos.repository.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.notification.NotificationEntity;

public interface NotificationRepository extends JpaRepository<NotificationEntity,Long>{

    
} 
