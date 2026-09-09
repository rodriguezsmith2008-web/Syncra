package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class AiSuggestedCardDTO {
    private String title;
    private String description;
    @JsonAlias("suggested_priority")
    private String suggestedPriority;
    @JsonAlias("column_id")
    private Long columnId;
    @JsonAlias("assigned_to")
    private Long assignedTo;
    @JsonAlias("due_date")
    private java.time.LocalDate dueDate;
}