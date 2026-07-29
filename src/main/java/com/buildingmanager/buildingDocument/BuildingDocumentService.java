package com.buildingmanager.buildingDocument;


import com.buildingmanager.building.Building;
import com.buildingmanager.building.BuildingRepository;
import com.buildingmanager.exceptions.BusinessValidationException;
import com.buildingmanager.fileStorage.LocalFileStorageService;
import com.buildingmanager.permission.BuildingPermissionService;
import com.buildingmanager.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingDocumentService {

    private final BuildingRepository buildingRepository;
    private final BuildingDocumentRepository documentRepository;
    private final BuildingDocumentMapper documentMapper;
    private final BuildingPermissionService buildingPermissionService;
    private final LocalFileStorageService fileStorageService;

    @Transactional
    public List<BuildingDocumentDTO> uploadDocuments(
            Integer buildingId,
            List<MultipartFile> files,
            BuildingDocumentCategory category,
            Authentication authentication
    ) {
        User user =
                (User) authentication.getPrincipal();

        Building building =
                buildingRepository.findById(buildingId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Building not found with id "
                                                + buildingId
                                )
                        );

        if (!buildingPermissionService
                .canManageBuilding(user, buildingId)) {

            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα μεταφόρτωσης "
                            + "αρχείων σε αυτή την πολυκατοικία."
            );
        }

        if (files == null || files.isEmpty()) {
            throw new BusinessValidationException(
                    "Πρέπει να επιλέξετε τουλάχιστον ένα αρχείο."
            );
        }

        List<BuildingDocument> documents =
                new ArrayList<>();

        List<Path> createdPhysicalFiles =
                new ArrayList<>();

        /*
         * Αν αποτύχει ή γίνει rollback το transaction,
         * διαγράφονται τα physical files του batch.
         */
        registerRollbackCleanup(createdPhysicalFiles);

        for (MultipartFile file : files) {
            LocalFileStorageService.StoredFile storedFile =
                    fileStorageService
                            .storeBuildingDocument(
                                    buildingId,
                                    file
                            );

            createdPhysicalFiles.add(
                    storedFile.physicalPath()
            );

            if (documentRepository
                    .existsByBuilding_IdAndChecksumSha256(
                            buildingId,
                            storedFile.checksumSha256()
                    )) {

                fileStorageService.deleteQuietly(
                        storedFile.physicalPath()
                );

                createdPhysicalFiles.remove(
                        storedFile.physicalPath()
                );

                throw new BusinessValidationException(
                        "Το αρχείο "
                                + storedFile.originalFileName()
                                + " έχει ήδη ανέβει "
                                + "στη συγκεκριμένη πολυκατοικία."
                );
            }

            BuildingDocument document =
                    BuildingDocument.builder()
                            .building(building)
                            .fileName(
                                    storedFile.originalFileName()
                            )
                            .storedFileName(
                                    storedFile.storedFileName()
                            )
                            .fileUrl(
                                    storedFile.publicUrl()
                            )
                            .contentType(
                                    storedFile.contentType()
                            )
                            .sizeBytes(
                                    storedFile.sizeBytes()
                            )
                            .checksumSha256(
                                    storedFile.checksumSha256()
                            )
                            .category(category)
                            .build();

            documents.add(document);
        }

        return documentRepository
                .saveAll(documents)
                .stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BuildingDocumentDTO> getDocuments(
            Integer buildingId,
            Authentication authentication
    ) {
        User user =
                (User) authentication.getPrincipal();

        if (!buildingRepository.existsById(buildingId)) {
            throw new EntityNotFoundException(
                    "Building not found with id "
                            + buildingId
            );
        }

        if (!buildingPermissionService
                .canViewBuilding(user, buildingId)) {

            throw new AccessDeniedException(
                    "Δεν έχετε πρόσβαση στα αρχεία "
                            + "αυτής της πολυκατοικίας."
            );
        }

        return getDocumentsInternal(buildingId);
    }

    @Transactional(readOnly = true)
    public List<BuildingDocumentDTO>
    getDocumentsInternal(Integer buildingId) {

        return documentRepository
                .findAllByBuilding_IdOrderByUploadedAtDesc(
                        buildingId
                )
                .stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteDocument(
            Integer buildingId,
            Integer documentId,
            Authentication authentication
    ) {
        User user =
                (User) authentication.getPrincipal();

        BuildingDocument document =
                documentRepository
                        .findByIdAndBuilding_Id(
                                documentId,
                                buildingId
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Το document δεν βρέθηκε "
                                                + "στη συγκεκριμένη πολυκατοικία."
                                )
                        );

        if (!buildingPermissionService
                .canManageBuilding(user, buildingId)) {

            throw new AccessDeniedException(
                    "Δεν έχετε δικαίωμα διαγραφής "
                            + "αρχείων από αυτή την πολυκατοικία."
            );
        }

        String storedFileName =
                document.getStoredFileName();

        documentRepository.delete(document);

        /*
         * Το physical file διαγράφεται μόνο αφού γίνει
         * επιτυχώς commit η διαγραφή του database record.
         */
        registerAfterCommitDeletion(
                buildingId,
                storedFileName
        );
    }

    private void registerRollbackCleanup(
            List<Path> createdFiles
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(
                                    int status
                            ) {
                                if (status
                                        == STATUS_ROLLED_BACK) {

                                    createdFiles.forEach(
                                            fileStorageService
                                                    ::deleteQuietly
                                    );
                                }
                            }
                        }
                );
    }

    private void registerAfterCommitDeletion(
            Integer buildingId,
            String storedFileName
    ) {
        if (!TransactionSynchronizationManager
                .isSynchronizationActive()) {

            fileStorageService
                    .deleteBuildingDocument(
                            buildingId,
                            storedFileName
                    );

            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                fileStorageService
                                        .deleteBuildingDocument(
                                                buildingId,
                                                storedFileName
                                        );
                            }
                        }
                );
    }
}
