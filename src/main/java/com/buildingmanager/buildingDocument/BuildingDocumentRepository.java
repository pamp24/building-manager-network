package com.buildingmanager.buildingDocument;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BuildingDocumentRepository
        extends JpaRepository<BuildingDocument, Integer> {

    List<BuildingDocument>
    findAllByBuilding_IdOrderByUploadedAtDesc(
            Integer buildingId
    );

    Optional<BuildingDocument>
    findByIdAndBuilding_Id(
            Integer documentId,
            Integer buildingId
    );

    boolean existsByBuilding_IdAndChecksumSha256(
            Integer buildingId,
            String checksumSha256
    );
}
