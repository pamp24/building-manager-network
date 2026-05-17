package com.buildingmanager.professional.professionalImage;

import com.buildingmanager.professional.ProfessionalBusiness;
import com.buildingmanager.professional.ProfessionalBusinessRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfessionalImageService {

    private final ProfessionalImageRepository imageRepository;
    private final ProfessionalBusinessRepository professionalRepository;

    @Value("${app.upload.professionals-dir:uploads/professionals}")
    private String uploadDir;

    public ProfessionalImageDTO uploadImage(Integer professionalId, MultipartFile file, boolean primaryImage) {
        ProfessionalBusiness professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Professional not found"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Το αρχείο είναι κενό.");
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Επιτρέπονται μόνο εικόνες.");
        }

        try {
            Path dir = Paths.get(uploadDir, String.valueOf(professionalId));
            Files.createDirectories(dir);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
            String extension = getExtension(originalName);
            String storedName = UUID.randomUUID() + extension;

            Path target = dir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            if (primaryImage) {
                unsetPrimaryImages(professionalId);
            }

            String imageUrl = "/uploads/professionals/" + professionalId + "/" + storedName;

            ProfessionalImage image = ProfessionalImage.builder()
                    .professional(professional)
                    .imageUrl(imageUrl)
                    .fileName(storedName)
                    .contentType(contentType)
                    .primaryImage(primaryImage)
                    .build();

            return toDTO(imageRepository.save(image));

        } catch (IOException e) {
            throw new RuntimeException("Αποτυχία αποθήκευσης εικόνας.", e);
        }
    }

    public List<ProfessionalImageDTO> getImages(Integer professionalId) {
        return imageRepository.findByProfessional_IdOrderByPrimaryImageDescCreatedAtDesc(professionalId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void deleteImage(Integer imageId) {
        ProfessionalImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));

        try {
            Path path = Paths.get(uploadDir)
                    .resolve(String.valueOf(image.getProfessional().getId()))
                    .resolve(image.getFileName());

            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }

        imageRepository.delete(image);
    }

    public ProfessionalImageDTO setPrimary(Integer imageId) {
        ProfessionalImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image not found"));

        Integer professionalId = image.getProfessional().getId();

        unsetPrimaryImages(professionalId);

        image.setPrimaryImage(true);

        return toDTO(imageRepository.save(image));
    }

    private void unsetPrimaryImages(Integer professionalId) {
        List<ProfessionalImage> images = imageRepository.findByProfessional_Id(professionalId);

        images.forEach(img -> img.setPrimaryImage(false));

        imageRepository.saveAll(images);
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index >= 0 ? filename.substring(index) : "";
    }

    private ProfessionalImageDTO toDTO(ProfessionalImage image) {
        return ProfessionalImageDTO.builder()
                .id(image.getId())
                .professionalId(image.getProfessional().getId())
                .imageUrl(image.getImageUrl())
                .fileName(image.getFileName())
                .contentType(image.getContentType())
                .primaryImage(image.isPrimaryImage())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
