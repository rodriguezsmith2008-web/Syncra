package com.syncra.gestion_proyectos.repository.sprint;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.sprint.SprintEntity;

public interface SprintRepository extends JpaRepository<SprintEntity,Long>{
    
}
