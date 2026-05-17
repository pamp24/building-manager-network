package com.buildingmanager.professional.professionalReview;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalReviewDTO {

    private Integer id;
    private Integer professionalId;

    private Integer rating;
    private String comment;

    private Integer reviewerId;
    private String reviewerName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
