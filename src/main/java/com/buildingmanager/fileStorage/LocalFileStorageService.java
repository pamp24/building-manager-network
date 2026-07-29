package com.buildingmanager.fileStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "jpg",
            "jpeg",
            "png",
            "webp",
            "doc",
            "docx",
            "xls",
            "xlsx"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",

            "image/jpeg",
            "image/png",
            "image/webp",

            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",

            /*
             * Μερικοί browsers/clients στέλνουν generic type.
             * Η επέκταση εξακολουθεί να ελέγχεται.
             */
            "application/octet-stream"
    );

    private final FileStorageProperties properties;

    public StoredFile storeBuildingDocument(
            Integer buildingId,
            MultipartFile file
    ) {
        validateFile(file);

        String originalFileName =
                sanitizeOriginalFileName(
                        file.getOriginalFilename()
                );

        String extension =
                extractExtension(originalFileName);

        String storedFileName =
                UUID.randomUUID() + "." + extension;

        Path buildingDirectory =
                getBuildingDirectory(buildingId);

        Path destination =
                buildingDirectory
                        .resolve(storedFileName)
                        .normalize();

        ensureInsideDirectory(
                buildingDirectory,
                destination
        );

        try {
            Files.createDirectories(buildingDirectory);

            String checksum = calculateSha256(file);

            try (InputStream inputStream =
                         file.getInputStream()) {

                Files.copy(
                        inputStream,
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            String publicUrl =
                    normalizePublicPrefix(
                            properties.getPublicPrefix()
                    )
                            + "/buildings/"
                            + buildingId
                            + "/"
                            + storedFileName;

            return new StoredFile(
                    originalFileName,
                    storedFileName,
                    publicUrl,
                    normalizeContentType(
                            file.getContentType()
                    ),
                    file.getSize(),
                    checksum,
                    destination
            );

        } catch (IOException exception) {
            throw new FileStorageException(
                    "Αποτυχία αποθήκευσης του αρχείου "
                            + originalFileName,
                    exception
            );
        }
    }

    public void deleteBuildingDocument(
            Integer buildingId,
            String storedFileName
    ) {
        Path buildingDirectory =
                getBuildingDirectory(buildingId);

        Path filePath =
                buildingDirectory
                        .resolve(storedFileName)
                        .normalize();

        ensureInsideDirectory(
                buildingDirectory,
                filePath
        );

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new FileStorageException(
                    "Αποτυχία διαγραφής του physical file.",
                    exception
            );
        }
    }

    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Χρησιμοποιείται μόνο για rollback cleanup.
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(
                    "Δεν επιτρέπεται κενό αρχείο."
            );
        }

        if (file.getSize()
                > properties.getMaxFileSizeBytes()) {

            throw new FileStorageException(
                    "Το αρχείο "
                            + file.getOriginalFilename()
                            + " ξεπερνά το μέγιστο μέγεθος των 10MB."
            );
        }

        String originalFileName =
                sanitizeOriginalFileName(
                        file.getOriginalFilename()
                );

        String extension =
                extractExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileStorageException(
                    "Ο τύπος αρχείου ."
                            + extension
                            + " δεν υποστηρίζεται."
            );
        }

        String contentType =
                normalizeContentType(file.getContentType());

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "Το content type "
                            + contentType
                            + " δεν υποστηρίζεται."
            );
        }
    }

    private String calculateSha256(
            MultipartFile file
    ) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            try (InputStream inputStream =
                         file.getInputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead =
                        inputStream.read(buffer)) != -1) {
                    digest.update(
                            buffer,
                            0,
                            bytesRead
                    );
                }
            }

            return HexFormat
                    .of()
                    .formatHex(digest.digest());

        } catch (NoSuchAlgorithmException
                 | IOException exception) {

            throw new FileStorageException(
                    "Δεν ήταν δυνατός ο έλεγχος του αρχείου.",
                    exception
            );
        }
    }

    private Path getBuildingDirectory(
            Integer buildingId
    ) {
        Path root =
                Paths.get(properties.getRootDirectory())
                        .toAbsolutePath()
                        .normalize();

        return root
                .resolve("buildings")
                .resolve(String.valueOf(buildingId))
                .normalize();
    }

    private void ensureInsideDirectory(
            Path directory,
            Path file
    ) {
        if (!file.startsWith(directory)) {
            throw new FileStorageException(
                    "Μη έγκυρη διαδρομή αρχείου."
            );
        }
    }

    private String sanitizeOriginalFileName(
            String originalFileName
    ) {
        if (originalFileName == null
                || originalFileName.isBlank()) {
            throw new FileStorageException(
                    "Το αρχείο δεν έχει έγκυρο όνομα."
            );
        }

        String cleanName =
                Paths.get(originalFileName)
                        .getFileName()
                        .toString()
                        .trim();

        if (cleanName.isBlank()) {
            throw new FileStorageException(
                    "Το αρχείο δεν έχει έγκυρο όνομα."
            );
        }

        return cleanName;
    }

    private String extractExtension(
            String fileName
    ) {
        int dotIndex =
                fileName.lastIndexOf('.');

        if (dotIndex < 0
                || dotIndex == fileName.length() - 1) {
            throw new FileStorageException(
                    "Το αρχείο δεν έχει έγκυρη επέκταση."
            );
        }

        return fileName
                .substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(
            String contentType
    ) {
        if (contentType == null
                || contentType.isBlank()) {
            return "application/octet-stream";
        }

        return contentType
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizePublicPrefix(
            String prefix
    ) {
        if (prefix == null
                || prefix.isBlank()) {
            return "/uploads";
        }

        String normalized = prefix.trim();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        while (normalized.endsWith("/")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        return normalized;
    }

    public record StoredFile(
            String originalFileName,
            String storedFileName,
            String publicUrl,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            Path physicalPath
    ) {
    }
}