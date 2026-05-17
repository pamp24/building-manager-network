package com.buildingmanager.professional.professionalImage;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProfessionalImageDTO {

    private Integer id;
    private Integer professionalId;
    private String imageUrl;
    private String fileName;
    private String contentType;
    private boolean primaryImage;
    private LocalDateTime createdAt;
}
