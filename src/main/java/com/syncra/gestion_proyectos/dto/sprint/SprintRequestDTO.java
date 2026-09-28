package com.syncra.gestion_proyectos.dto.sprint;

import java.time.LocalDate;

import lombok.Data;

@Data

public class SprintRequestDTO {

    private Long projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

}
