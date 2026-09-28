package com.syncra.gestion_proyectos.dto.ai;

import java.time.LocalDate;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class AiActionDTO {
    @JsonAlias({"action", "type"}) private String type;
    @JsonAlias("task_id") private Long taskId;
    @JsonAlias("document_id") private Long documentId;
    @JsonAlias("project_id") private Long projectId;
    @JsonAlias("column_id") private Long columnId;
    @JsonAlias("assigned_to") private Long assignedTo;
    private String priority;
    @JsonAlias("sprint_id") private Long sprintId;
    private Long position;
    private String title;
    private String description;
    private String content;
    private String documentType;
    private String color;
    @JsonAlias("due_date") private LocalDate dueDate;
}