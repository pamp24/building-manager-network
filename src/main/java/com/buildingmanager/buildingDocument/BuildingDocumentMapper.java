package com.buildingmanager.buildingDocument;


import org.springframework.stereotype.Component;

@Component
public class BuildingDocumentMapper {

    public BuildingDocumentDTO toDto(
            BuildingDocument document
    ) {
        return BuildingDocumentDTO.builder()
                .id(document.getId())
                .buildingId(
                        document.getBuilding().getId()
                )
                .fileName(document.getFileName())
                .fileUrl(document.getFileUrl())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .uploadedAt(document.getUploadedAt())
                .category(document.getCategory())
                .build();
    }
}