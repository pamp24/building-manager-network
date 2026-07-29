package com.buildingmanager.buildingDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDocumentDTO {

    private Integer id;
    private Integer buildingId;
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime uploadedAt;
    private BuildingDocumentCategory category;
}
