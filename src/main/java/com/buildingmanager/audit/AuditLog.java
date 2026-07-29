package com.buildingmanager.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer actorId;

    private String actorEmail;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    private String entityType;

    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(length = 1000)
    private String description;

    private LocalDateTime timestamp;

    private String correlationId;
}
