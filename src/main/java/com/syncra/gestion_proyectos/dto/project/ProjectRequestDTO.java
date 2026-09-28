package com.syncra.gestion_proyectos.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequestDTO {

    @NotBlank(message = "El nombre del proyecto es obligatorio")
    private String name;

    private String description;

    @NotBlank(message = "El grupo (ficha) es obligatorio")
    private String groupName;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate endDate;

    // El campo status no se recibe, se asigna por defecto en el servicio
}