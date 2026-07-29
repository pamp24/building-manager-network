package com.buildingmanager.buildingDocument;

import com.buildingmanager.building.Building;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "building_document",
        indexes = {
                @Index(
                        name = "idx_building_document_building",
                        columnList = "building_id"
                ),
                @Index(
                        name = "idx_building_document_checksum",
                        columnList = "building_id, checksum_sha256"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "building_id",
            nullable = false
    )
    private Building building;


    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private BuildingDocumentCategory category;

    /*
     * Χρησιμοποιείται για πραγματικό duplicate detection.
     */
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
