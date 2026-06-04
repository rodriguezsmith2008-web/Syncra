package com.syncra.gestion_proyectos.repository.access;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.access.AcessRequestEntity;

@Repository

public interface AcessRequestRepository extends JpaRepository<AcessRequestEntity, Long> {
    
    

}
