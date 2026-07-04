package com.buildingmanager.professional;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalBusinessDTO {

    private Integer id;

    private String businessName;
    private String ownerFullName;

    private ProfessionalCategory category;
    private String description;

    private String phone;
    private String email;
    private String website;
    private String country;
    private String city;
    private String region;
    private String area;
    private String address;

    private String taxNumber;

    private boolean verified;
    private boolean active;

    private Double ratingAverage;
    private Integer reviewCount;

    private Integer createdByUserId;
    private String createdByUserName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String primaryImageUrl;

    private String workingHours;
}