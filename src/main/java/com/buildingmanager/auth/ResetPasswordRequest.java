package com.buildingmanager.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    private String token;
    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός")
    @Size(min = 8, message = "Ο κωδικός πρέπει να περιέχει τουλάχιστον 8 χαρακτήρες")
    private String newPassword;
}
