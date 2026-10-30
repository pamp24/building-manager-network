package com.buildingmanager.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class AuthenticationRequest {

    @Email(message = "Το email δεν είναι σωστά διαμορφωμένο")
    @NotBlank(message = "Το email είναι υποχρεωτικό")
    private String email;

    @NotBlank(message = "Ο κωδικός είναι υποχρεωτικός")
    @Size(min = 8, message = "Ο κωδικός πρέπει να περιέχει τουλάχιστον 8 χαρακτήρες")
    private String password;

}
