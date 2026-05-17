package com.buildingmanager.professional.professionalImage;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/professionals")
@RequiredArgsConstructor
public class ProfessionalImageController {

    private final ProfessionalImageService imageService;

    @PostMapping("/{professionalId}/images")
    public ProfessionalImageDTO uploadImage(
            @PathVariable Integer professionalId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primaryImage
    ) {
        return imageService.uploadImage(professionalId, file, primaryImage);
    }

    @GetMapping("/{professionalId}/images")
    public List<ProfessionalImageDTO> getImages(
            @PathVariable Integer professionalId
    ) {
        return imageService.getImages(professionalId);
    }

    @DeleteMapping("/images/{imageId}")
    public void deleteImage(
            @PathVariable Integer imageId
    ) {
        imageService.deleteImage(imageId);
    }

    @PatchMapping("/images/{imageId}/primary")
    public ProfessionalImageDTO setPrimary(
            @PathVariable Integer imageId
    ) {
        return imageService.setPrimary(imageId);
    }
}