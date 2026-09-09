package com.syncra.gestion_proyectos.dto.ai;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiTaskContextDTO {
    private Long id;
    private String title;
    private String description;
    private Long columnId;
    private Long sprintId;
    private LocalDate dueDate;
    private Long assignedTo;
}