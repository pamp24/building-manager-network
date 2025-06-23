package com.buildingmanager.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ForgotPasswordRequest {

    @Email(message = "Το email δεν είναι σωστά διαμορφωμένο")
    @NotBlank(message = "Το email είναι υποχρεωτικό")
    private String email;
}
