package com.syncra.gestion_proyectos.entity.document;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "document_comments")
public class DocumentCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "anchor_id", length = 100)
    private String anchorId;

    @Column(name = "anchor_text", length = 300)
    private String anchorText;
    
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
    
}
