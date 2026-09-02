package com.syncra.gestion_proyectos.dto.document;

import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Data;

/** DTO para actualizar el contenido de un documento */
@Data
public class DocumentUpdateDTO {

    private String title;
    private String content;
    private String coverImageUrl;
    private Long sprintId;
    private Integer quarter;

    private boolean coverImageUrlProvided;
    private boolean sprintIdProvided;
    private boolean quarterProvided;

    @JsonSetter("sprintId")
    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
        this.sprintIdProvided = true;
    }

    @JsonSetter("coverImageUrl")
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        this.coverImageUrlProvided = true;
    }

    @JsonSetter("quarter")
    public void setQuarter(Integer quarter) {
        this.quarter = quarter;
        this.quarterProvided = true;
    }
}
