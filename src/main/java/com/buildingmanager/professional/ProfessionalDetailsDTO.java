package com.buildingmanager.professional;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalDetailsDTO {

    private Integer id;

    private String businessName;

    private String description;

    private String category;

    private String phone;

    private String email;

    private String website;
    private String country;

    private String city;

    private String region;

    private Boolean verified;

    private Double ratingAverage;

    private Integer reviewCount;

    private String ownerFullName;

    private String coverImageUrl;
}