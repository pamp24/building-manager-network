package com.buildingmanager.buildingMember;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignApartmentRequest {
    @NotBlank
    private String role; // "Owner" ή "Resident"

    @NotNull
    private Integer apartmentId;
}
