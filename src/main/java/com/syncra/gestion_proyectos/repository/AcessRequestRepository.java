package com.syncra.gestion_proyectos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.AcessRequestEntity;

@Repository

public interface AcessRequestRepository extends JpaRepository<AcessRequestEntity, Long> {
    
    

}
