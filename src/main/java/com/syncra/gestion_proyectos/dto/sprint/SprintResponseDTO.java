package com.syncra.gestion_proyectos.dto.sprint;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.SprintStatusEnum;

import lombok.Data;

@Data
public class SprintResponseDTO {

    private Long id;
    private Long projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private SprintStatusEnum status;
    private LocalDateTime createdAt;

}

