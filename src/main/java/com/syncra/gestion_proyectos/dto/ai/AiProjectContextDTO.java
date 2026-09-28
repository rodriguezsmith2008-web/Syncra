package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class AiProjectContextDTO {
    @JsonProperty("project_id")
    private Long projectId;
    @JsonProperty("project_name")
    private String projectName;
    @JsonProperty("project_status")
    private String projectStatus;
    @JsonProperty("pending_tasks_summary")
    private String pendingTasksSummary;
    private List<AiMemberContextDTO> members;
    private List<AiColumnContextDTO> columns;
    private List<AiTaskContextDTO> tasks;
    private List<AiSprintContextDTO> sprints;
    private List<AiDocumentContextDTO> documents;
    @JsonProperty("current_user_id")
    private Long currentUserId;
    @JsonProperty("current_user_name")
    private String currentUserName;
}