package com.syncra.gestion_proyectos.repository.access;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.access.AccessRequestEntity;
import com.syncra.gestion_proyectos.enums.AccessStatusEnum;


@Repository
public interface AcessRequestRepository extends JpaRepository<AccessRequestEntity, Long> {

     //se usa para verificar si ya existe una solicitud pendiente con ese email antes de crear una nueva.
    boolean existsByEmailAndStatus(String email, AccessStatusEnum status);

   //se usa para que el admin vea todas las solicitudes PENDING. Si le pasas APPROVED te trae las aprobadas
    List<AccessRequestEntity> findAllByStatus(AccessStatusEnum status);

}