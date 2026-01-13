package com.buildingmanager.buildingMember;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BuildingMemberDTO {
    private Integer id;
    private Integer userId;
    private String fullName;
    private String email;
    private String role;
    private String status;
    private Integer buildingId;
    private String buildingName;
    private String apartmentNumber;
    private String floor;
}

