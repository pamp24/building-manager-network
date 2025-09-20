package com.buildingmanager.invite;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteRequestDTO {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String role;
    @NotBlank
    private Integer apartmentId;
}
